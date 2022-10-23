import * as functions from "firebase-functions";
import admin from "firebase-admin";
import {ZodError} from "zod";

import {WatchlistItem, WatchlistItemSchema} from "./models/WatchlistItem.js";
import {SteamApp, SteamAppSchema} from "./models/SteamApp.js";
import {GameWatcher, GameWatcherSchema} from "./models/GameWatcher.js";

import {getSteamPriceData} from "./utilities/steamApiFunctions.js";
import {sendPriceDropMessage} from "./utilities/sendPriceDropMessage.js";
import {attemptToCreateGame} from "./utilities/attemptToCreateGame.js";

// Refer to https://firebase.google.com/docs/admin/setup
// on how to obtain the Service Account private key
// If running locally and you want notifications:
// admin.initializeApp({
//   credential: admin.credential.cert("../secret.json"),
// });

admin.initializeApp();

const db = admin.firestore();

export const handleUserWatchlistItemWrite = functions.firestore
  .document("users/{uid}/watchlist/{appId}")
  .onWrite(async (change, context) => {
    const uid = context.params.uid;
    const appId = context.params.appId;

    if (typeof uid !== "string" || typeof appId !== "string") {
      functions.logger.error("Received invalid trigger params");
      functions.logger.error(`uid: ${uid}, appId: ${appId}`);
      return;
    }

    // A reference to the game the user is watching.
    const gameDocument = db.collection("games").doc(appId);
    // A reference to the corresponding watchlist item on the game.
    const gameWatcherDocument = gameDocument.collection("watchers").doc(uid);

    // If the item has been deleted, we delete the corresponding item in the
    // games collection and we are done.
    if (!change.after.exists) {
      return gameDocument.delete();
    }

    // We extract the new item.
    let newItem: WatchlistItem;
    try {
      newItem = WatchlistItemSchema.parse(change.after.data());
    } catch (error) {
      if (!(error instanceof ZodError)) {
        throw error;
      }

      functions.logger.error(
        "Received invalid new watchlist item, the security rules should have " +
          "prevented this"
      );
      functions.logger.error(error);

      return;
    }

    // If the game is not currently being watched by us, we need to add it.
    if (!(await gameDocument.get()).exists) {
      const intAppId = parseInt(appId);
      if (isNaN(intAppId)) {
        functions.logger.error(
          "Could not convert appId to a number, so could not populate global " +
            "record for game"
        );
      } else {
        await attemptToCreateGame(intAppId, gameDocument);
      }
    }

    const watcher: GameWatcher = {
      uid: uid,
      threshold: newItem.threshold,
    };

    // Update the watchlist item that is stored on the game to align with the
    // new or updated watchlist item on the user.
    return gameWatcherDocument.set(watcher);
  });

export const sendPriceChangeNotification = functions.firestore
  .document("games/{appId}")
  .onWrite(async (change, context) => {
    const appId = context.params.appId;
    functions.logger.log(`Detected write to app ID ${appId}`);

    // If the game is deleted for some reason, then we don't do anything
    // This behaviour might change in the future (alert the users that the
    // game is delisted?)
    if (!change.after.exists) {
      return;
    }

    // Check to see if prices have changed, this avoids messaging devices
    // on situations where app details other than price have been altered
    let beforeAppObj: SteamApp | undefined;
    let appObj: SteamApp;

    try {
      beforeAppObj = SteamAppSchema.optional().parse(change.before.data());
      appObj = SteamAppSchema.parse(change.after.data());
    } catch (error) {
      if (!(error instanceof ZodError)) {
        throw error;
      }

      functions.logger.error(
        "Received invalid SteamApp item, the security rules should have " +
          "prevented this"
      );
      functions.logger.error(error);

      return;
    }

    // TODO: decide what happens when a game goes free-to-play or vice versa
    // For the time being, just do nothing if the game is free to play
    // Checking free -> non-free doesn't make much sense, since a user is not
    // going to be tracking a free app (logically speaking)
    // And checking non-free -> free should probably be some sort of special
    // announcement if really needed. I can see this being relevant when a game
    // goes into a "free-to-play" week or something
    if (beforeAppObj?.isFree === true || appObj.isFree === true) {
      functions.logger.log("Game is/was now free to play, do nothing");
      return;
    }

    // Otherwise, we have priceData for both objects so...
    if (beforeAppObj?.priceData.final === appObj.priceData.final) {
      functions.logger.log(`Price information did not change for ${appId}`);
      return;
    }

    // Proceed to notify all watchers
    const price: number = appObj.priceData.final;
    const appName: string = appObj.name;
    const watchers = await db
      .doc(`games/${appId}`)
      .collection("watchers")
      .get();

    watchers.forEach(async (watcher) => {
      let watcherObj: GameWatcher;
      try {
        watcherObj = GameWatcherSchema.parse(watcher.data());
      } catch (error) {
        if (!(error instanceof ZodError)) {
          throw error;
        }

        functions.logger.error(
          "Received invalid SteamApp item, the security rules should have " +
            "prevented this"
        );
        functions.logger.error(error);

        return;
      }

      const uid = watcherObj.uid;
      const threshold = watcherObj.threshold;

      // Alert only users who meet the threshold criteria
      if (price < threshold) {
        functions.logger.log(
          `Alert the user ${uid} that the app is now ${price}`,
          `which is under ${threshold}`
        );

        // Get all device tokens of user and send notification
        const devices = await db
          .doc(`users/${uid}`)
          .collection("devices")
          .get();

        const devArr: Array<string> = [];

        devices.forEach((device) => {
          const devid = device.get("devid");
          devArr.push(devid);
          functions.logger.log(`Sending notification to ${devid}`);
        });
        sendPriceDropMessage(appName, appId, devArr, price, threshold);
      }
    });
  });

export const getPrices = functions.pubsub
  .schedule("every 2 hours")
  // .schedule("every 1 minutes") // to test
  .onRun(async () => {
    const gamesSnapshot = await db.collection("games").get();

    const appIds: string[] = [];
    gamesSnapshot.forEach((gameSnapshot) => appIds.push(gameSnapshot.id));

    const prices = await getSteamPriceData(appIds);

    gamesSnapshot.docs.forEach((gameDocument) => {
      // TODO: Check if this is a change before pushing, as we get more reads
      // than writes for free.

      const priceInformation = prices[gameDocument.id];
      if (priceInformation === undefined) {
        functions.logger.error(
          `Failed to get price information for app ID ${gameDocument.id}`
        );
        return;
      }

      gameDocument.ref.set(
        {
          ...priceInformation,
          updated: admin.firestore.Timestamp.now(),
        },
        {merge: true}
      );
    });
  });
