import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {ZodError} from "zod";

import {NewWatchlistItem, NewWatchlistItemSchema} from "./NewWatchlistItem";
import {
  ExistingWatchlistItem,
  ExistingWatchlistItemSchema,
} from "./ExisitngWatchlistItem";
import {watchlistItemsMatch} from "./watchlistItemsMatch";
import {SteamApp, SteamAppSchema} from "./SteamApp";
import {GameWatcher, GameWatcherSchema} from "./GameWatcher";

const serviceAccount = require("C:/Users/tonyh/Downloads/steamwhistlemobile-firebase-adminsdk-kj0dc-638f68e75a.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://steamwhistlemobile-default-rtdb.firebaseio.com",
});

// admin.initializeApp()
const db = admin.firestore();

async function sendPriceDropMessage(
  appName: string,
  appId: number,
  deviceToken: string,
  currentPrice: number,
  threshold: number
) {
  const msg =
    `You wanted ${appName} for less than ${threshold} ` +
    `well now it's available at a price of ${currentPrice}!`;

  const payload = {
    notification: {
      title: `Price Drop Alert for ${appName}`,
      body: msg,
      click_action: "OPEN_WATCHLIST",
    },
    data: {
      appName: appName,
      appId: `${appId}`,
      currentPrice: `${currentPrice}`,
      threshold: `${threshold}`,
    },
  };

  // Send notifications to all tokens.
  // TODO: Optimise to do a multicast message instead of multiple
  // single messages which is slower/incurs more cost, OK for testing though
  try {
    await admin.messaging().sendToDevice(deviceToken, payload);
  } catch (error) {
    functions.logger.log(error);
  }
}

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

    // A reference to the corresponding watchlist item on the game.
    const gameDocument = db.doc(`games/${appId}/watchers/${uid}`);

    // If the item has been deleted, we delete the corresponding item in the
    // games collection and we are done.
    if (!change.after.exists) {
      return gameDocument.delete();
    }

    // We extract the new item.
    let newItem: NewWatchlistItem;
    try {
      newItem = NewWatchlistItemSchema.parse(change.after.data());
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

    const now = admin.firestore.Timestamp.now();

    // If we are doing an update, we need to make sure that we only update the
    // updated timestamp if the main parts of the document have actually
    // changed, as otherwise we could end up in an infinite loop where setting
    // the updated timestamp triggers this function again, which sets the
    // updated timestamp and triggers this function again, etc. etc. As such a
    // loop may cost me actual money, I would like to avoid it.
    if (change.before.exists) {
      // We extract the old item.
      let oldItem: ExistingWatchlistItem;
      try {
        oldItem = ExistingWatchlistItemSchema.parse(change.before.data());
      } catch (error) {
        if (!(error instanceof ZodError)) {
          throw error;
        }

        functions.logger.error(
          "Received invalid old watchlist item in a change, the security " +
            "rules should have prevented this"
        );
        functions.logger.error(error);

        return;
      }

      if (watchlistItemsMatch(oldItem, newItem)) {
        // No meaningful update has occurred. We don't need to do anything. In
        // fact we _need_ to not do anything.
        return;
      }

      // The item has been meaningfully updated. Update the updated time.
      await change.after.ref.set({updated: now}, {merge: true});
    } else {
      // The item has just been created. Add the updated and created times, and
      // the set them to now.
      await change.after.ref.set({created: now, updated: now}, {merge: true});
    }

    // Update the watchlist item that is stored on the game to align with the
    // new or (meaningfully) updated watchlist item on the user.
    return db.doc(`games/${appId}/watchers/${uid}`).set(newItem);
  });

export const sendPriceChangeNotification = functions.firestore
  .document("games/{appId}")
  .onWrite(async (change, context) => {
    const appId = context.params.appId;

    // If the game is deleted for some reason, then we don't do anything
    // This behaviour might change in the future (alert the users that the
    // game is delisted?)
    if (!change.after.exists) {
      return;
    }

    // Check to see if prices have changed, this avoids messaging devices
    // on situations where app details other than price have been altered
    let beforeAppObj: SteamApp;
    let appObj: SteamApp;

    try {
      beforeAppObj = SteamAppSchema.parse(change.before.data());
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

    if (beforeAppObj.priceData.final === appObj.priceData.final) {
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
          `Alert the user ${uid} that the app is now ${threshold}`,
          `which is under ${price}`
        );

        // Get all device tokens of user and send notification
        const devices = await db
          .doc(`users/${uid}`)
          .collection("devices")
          .get();

        devices.forEach((device) => {
          const devid = device.get("devid");
          sendPriceDropMessage(appName, appId, devid, price, threshold);
        });
      }
    });
  });
