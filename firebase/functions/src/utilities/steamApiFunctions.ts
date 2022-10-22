import {logger} from "firebase-functions";
import {ZodError} from "zod";
import fetch from "node-fetch";

import {
  SteamApiPriceOverview,
  SteamApiGamePriceAndNameData,
  SteamApiGamePriceAndNameDataSchema,
  SteamApiGamePriceData,
  SteamApiGamePriceDataSchema,
} from "../models/steamApiResponses.js";
import {
  PriceData,
  GamePriceInformation,
  Game,
} from "../models/gameInformation.js";

const URL = "https://store.steampowered.com/api/appdetails";

async function getSteamJsonRespose(urlQuery: string): Promise<object> {
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

  return json;
}

function getPriceData(
  priceOverview: SteamApiPriceOverview
): [isFree: boolean, priceData: PriceData] {
  if (priceOverview === undefined) {
    // No price overview means the game is free.
    return [true, {
      final: 0,
      initial: 0,
      discountPercentage: 0,
    }];
  } else {
    if (priceOverview.currency !== "AUD") {
      // TODO: Actually handle this.
      logger.warn(
        `Got non-AUD currency of ${priceOverview.currency}`
      );
    }

    return [false, {
      final: priceOverview.final,
      initial: priceOverview.initial,
      discountPercentage:
        priceOverview.discount_percent,
    }];
  }
}

/**
 * Get the price and name data of a single app from steam. This is a separate
 * function because if you want data on multiple games you can only get price
 * data.
 * @param {number} appId The appId of the game to get data on.
 */
export async function getSteamGameData(
  appId: number
): Promise<Game | null> {
  const urlQuery = `${URL}?appids=${appId}&filters=basic,price_overview&cc=au`;
  logger.info("Steam url to query for price and name", urlQuery);

  const json = await getSteamJsonRespose(urlQuery);

  const results: {[appId: string]: Game} = {};
  for (const [currentAppId, rawResponse] of Object.entries(json)) {
    let response: SteamApiGamePriceAndNameData;
    try {
      response = SteamApiGamePriceAndNameDataSchema.parse(
        rawResponse
      );
    } catch (error) {
      if (!(error instanceof ZodError)) {
        throw error;
      }

      logger.error(
        `Got unexpected response from steam for app ID ${currentAppId}`
      );
      logger.error(error);
      continue;
    }

    if (!response.success) {
      logger.error(`Got failed response from steam for app ID ${currentAppId}`);
      continue;
    }

    const [isFree, priceData] = getPriceData(response.data.price_overview);
    results[currentAppId] = {
      appId,
      name: response.data.name,
      isFree,
      priceData,
    };
  }

  const result = results[appId];
  if (result === undefined) {
    logger.error(`Did not get steam response for expected app ID ${appId}`);
    logger.error("Data from steam response", results);
    return null;
  }
  return result;
}

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
  logger.info("Steam url to query for price data", urlQuery);

  const json = await getSteamJsonRespose(urlQuery);

  const results: {[appId: string]: GamePriceInformation} = {};

  for (const [currentAppId, rawIndividualResponse] of Object.entries(json)) {
    let individualResponse: SteamApiGamePriceData;
    try {
      individualResponse = SteamApiGamePriceDataSchema.parse(
        rawIndividualResponse
      );
    } catch (error) {
      if (!(error instanceof ZodError)) {
        throw error;
      }

      logger.error(
        `Got unexpected response from steam for app ID ${currentAppId}`
      );
      logger.error(error);
      continue;
    }

    if (!individualResponse.success) {
      logger.error(`Got failed response from steam for app ID ${currentAppId}`);
      continue;
    }

    const [isFree, priceData] = getPriceData(
      individualResponse.data.price_overview
    );
    results[currentAppId] = {
      isFree,
      priceData,
    };
  }

  return results;
}
