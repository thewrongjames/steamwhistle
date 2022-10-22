import admin from "firebase-admin";
import {logger} from "firebase-functions";

import {getSteamGameData} from "./steamApiFunctions.js";

/**
 * Attempt to get data on a given game from steam, and insert that data at the
 * given document reference in firestore.
 * @param {number} appId The app ID to use to get the data from steam.
 * @param {admin.firestore.DocumentReference} gameDocument A reference to insert
 * the game data at.
 * @return {Promise<void>} A promise that resolves when the attempt is
 * complete.
 */
export async function attemptToCreateGame(
  appId: number,
  gameDocument: admin.firestore.DocumentReference,
): Promise<void> {
  logger.log(`Attempting to globally add game ${appId}`);

  const game = await getSteamGameData(appId);
  if (game === null) {
    logger.error("Unable to get game data from steam");
    return;
  }

  await gameDocument.create({
    ...game,
    created: admin.firestore.Timestamp.now(),
    updated: admin.firestore.Timestamp.now(),
  });
}
