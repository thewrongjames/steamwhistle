import {z} from "zod";

export const NewWatchlistItemSchema = z.object({
  appId: z.number().positive().int(),
  threshold: z.number().positive().int(),
});

export type NewWatchlistItem = z.infer<typeof NewWatchlistItemSchema>;
