import * as functions from "firebase-functions";
import admin from "firebase-admin";

export async function sendPriceDropMessage(
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
    functions.logger.error(
      "You might need to set the path to your " +
        "service account key, see comments for instructions"
    );
    functions.logger.error(error);
  }
}
