import {z} from "zod";

export const SteamAppSchema = z.object({
  isFree: z.boolean(),
  name: z.string(),
  created: z.date().optional(),
  updated: z.date().optional(),
  priceData: z.object({
    discountPercentage: z.number().nonnegative().int(),
    final: z.number().nonnegative().int(),
    initial: z.number().nonnegative().int(),
  }),
});

export type SteamApp = z.infer<typeof SteamAppSchema>;
