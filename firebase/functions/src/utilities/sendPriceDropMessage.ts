import * as functions from "firebase-functions";
import admin from "firebase-admin";

function centsToMoneyString(cents: number) {
  return `$${(cents / 100).toFixed(2)}`;
}

export async function sendPriceDropMessage(
  appName: string,
  appId: number,
  devices: Array<string>,
  currentPrice: number,
  threshold: number
) {
  const msg =
    `You wanted ${appName} for less than ${centsToMoneyString(threshold)} ` +
    "well now it's available at a price of " +
    `${centsToMoneyString(currentPrice)}!`;

  const payload: admin.messaging.MulticastMessage = {
    tokens: devices,
    notification: {
      title: `Price Drop Alert for ${appName}`,
      body: msg,
    },
    data: {
      appName: appName,
      appId: `${appId}`,
      currentPrice: `${currentPrice}`,
      threshold: `${threshold}`,
    },
    android: {
      priority: "high",
      notification: {
        clickAction: "OPEN_WATCHLIST",
      },
    },
    apns: {
      headers: {
        "apns-priority": "10",
      },
    },
    webpush: {
      headers: {
        Urgency: "high",
      },
    },
  };

  // Send notifications to all tokens.
  // TODO: Optimise to do a multicast message instead of multiple
  // single messages which is slower/incurs more cost, OK for testing though
  try {
    await admin.messaging().sendMulticast(payload);
    functions.logger.log("Send to all devices completed.");
    functions.logger.log(devices);
  } catch (error) {
    functions.logger.error("Something went wrong");
    functions.logger.log(error);
  }
}
