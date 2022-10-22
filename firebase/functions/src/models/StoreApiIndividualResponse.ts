import {z} from "zod";

/**
 * The Zod schema for the response we expect to get from the steam store API for
 * each app ID we give it.
 */
export const StoreApiIndividualResponseSchema = z.object({
  success: z.boolean(),
  data: z.object({
    // This will not exist if the game is free, using the is_free property would
    // be a better way to tell if the game is free, but if we want to get
    // multiple app IDs at once we can only query the price_overview field.
    // See https://wiki.teamfortress.com/wiki/User:RJackson/StorefrontAPI#appdetails.
    price_overview: z.object({
      currency: z.string(),
      initial: z.number().int(),
      final: z.number().int(),
      discount_percent: z.number(),
    }).optional(),
  }),
});

export type StoreApiIndividualResponse =
  z.infer<typeof StoreApiIndividualResponseSchema>;
