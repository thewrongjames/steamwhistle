import {z} from "zod";

// This will not exist if the game is free, using the is_free property would
// be a better way to tell if the game is free, but if we want to get
// multiple app IDs at once we can only query the price_overview field.
// See https://wiki.teamfortress.com/wiki/User:RJackson/StorefrontAPI#appdetails.
export const SteamApiPriceOverviewSchema = z.object({
  currency: z.string(),
  initial: z.number().int(),
  final: z.number().int(),
  discount_percent: z.number(),
}).optional();

export type SteamApiPriceOverview =
  z.infer<typeof SteamApiPriceOverviewSchema>

/**
 * The Zod schema for the response we expect to get from the steam store API for
 * each app ID we give it when getting just price data.
 */
export const SteamApiGamePriceDataSchema = z.object({
  success: z.boolean(),
  data: z.object({
    price_overview: SteamApiPriceOverviewSchema,
  }),
});

export type SteamApiGamePriceData =
  z.infer<typeof SteamApiGamePriceDataSchema>;

export const SteamApiGamePriceAndNameDataSchema = z.object({
  success: z.boolean(),
  data: z.object({
    name: z.string(),
    price_overview: SteamApiPriceOverviewSchema,
  }),
});

export type SteamApiGamePriceAndNameData =
  z.infer<typeof SteamApiGamePriceAndNameDataSchema>
