import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {ZodError} from "zod";

import {NewWatchlistItem, NewWatchlistItemSchema} from "./NewWatchlistItem";
import {
  ExistingWatchlistItem,
  ExistingWatchlistItemSchema,
} from "./ExisitngWatchlistItem";
import {watchlistItemsMatch} from "./watchlistItemsMatch";

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
