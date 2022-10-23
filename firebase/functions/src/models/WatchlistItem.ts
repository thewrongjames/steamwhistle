import {z} from "zod";

export const WatchlistItemSchema = z.object({
  appId: z.number().int(),
  threshold: z.number().int(),
  isActive: z.boolean(),
});

export type WatchlistItem = z.infer<typeof WatchlistItemSchema>;
