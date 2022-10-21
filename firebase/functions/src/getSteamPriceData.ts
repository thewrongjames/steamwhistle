import {logger} from "firebase-functions";
import {ZodError} from "zod";
import fetch from "node-fetch";

import {GamePriceInformation} from "./GamePriceInformation.js";
import {
  StoreApiIndividualResponseSchema,
} from "./StoreApiIndividualResponse.js";

const URL = "https://store.steampowered.com/api/appdetails";

/**
 * Gets the steam price data for a given list of steam app IDs. It errors if it
 * gets an invalid response from steam. It is not, in principle, guaranteed to
 * return the same appIds as it was given, it just gives back what it got from
 * steam (all be it type-validated)
 * @param {string[]} appIds The strings of the app IDs of the apps to get the
 * price data for.
 */
export async function getSteamPriceData(
  appIds: string[]
): Promise<{[appId: string]: GamePriceInformation}> {
  const urlQuery = `${URL}?appids=${appIds.join(",")}` +
    "&filters=price_overview&cc=au";
  logger.info("Steam url to query", urlQuery);
  const response = await fetch(urlQuery);

  if (!response.ok) {
    logger.error(response);
    throw Error(
      `Got not-okay response from steam; status: ${response.status}; ` +
      `status text: ${response.statusText};`
    );
  }

  const json = await response.json();
  logger.log("Steam api response", json);
  if (typeof json !== "object" || json == null) {
    throw Error("Got non-object json data from steam.");
  }

  const results: {[appId: string]: GamePriceInformation} = {};

  for (const [appId, rawIndividualResponse] of Object.entries(json)) {
    let individualResponse;
    try {
      individualResponse = StoreApiIndividualResponseSchema.parse(
        rawIndividualResponse
      );
    } catch (error) {
      if (!(error instanceof ZodError)) {
        throw error;
      }

      logger.error(`Got unexpected response from steam for app ID ${appId}`);
      logger.error(error);
      continue;
    }

    if (!individualResponse.success) {
      logger.error(`Got failed response from steam for app ID ${appId}`);
      continue;
    }

    if (individualResponse.data.price_overview === undefined) {
      // No price overview means the game is free. It would be better to be able
      // to check the is_free field, but if we want to get multiple app IDs at
      // once we can only query the price_overview field.
      // See https://wiki.teamfortress.com/wiki/User:RJackson/StorefrontAPI#appdetails.
      results[appId] = {
        isFree: true,
        priceData: {
          final: 0,
          initial: 0,
          discountPercentage: 0,
        },
      };
    } else {
      if (individualResponse.data.price_overview.currency !== "AUD") {
        // TODO: Actually handle this.
        logger.warn(
          "Got non-AUD currency of " +
          `${individualResponse.data.price_overview.currency} for app ID ` +
          appId
        );
      }

      results[appId] = {
        isFree: false,
        priceData: {
          final: individualResponse.data.price_overview.final,
          initial: individualResponse.data.price_overview.initial,
          discountPercentage:
            individualResponse.data.price_overview.discount_percent,
        },
      };
    }
  }

  return results;
}
