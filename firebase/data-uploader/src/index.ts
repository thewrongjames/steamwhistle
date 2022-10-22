import * as fs from "node:fs";
import {GameDataSchema} from "./GameData.js";

import admin from "firebase-admin";

const DATA_FILE_NAME = "data.json";
const COLLECTION_NAME = "steamGames";

admin.initializeApp({
  credential: admin.credential.cert("../secret.json"),
});
const db = admin.firestore();


const rawGameData = JSON.parse(fs.readFileSync(DATA_FILE_NAME, "utf-8"));
// Just throw the ZodError if there is a problem, this is a script for
// developers to run.
const gameData = GameDataSchema.parse(rawGameData);

for (const game of gameData.response.apps) {
  const document = db.collection(COLLECTION_NAME).doc("" + game.appid);

  // We first check if the document exists, because we have more free reads than
  // writes. If it does, we don't write.
  if ((await document.get()).exists) {
    continue;
  }

  await document.set({
    appId: game.appid,
    name: game.name,
  });
}
