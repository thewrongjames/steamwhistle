import {z} from "zod";
import {NewWatchlistItemSchema} from "./NewWatchlistItem.js";

export const ExistingWatchlistItemSchema = NewWatchlistItemSchema.extend({
  created: z.date(),
  updated: z.date(),
});

export type ExistingWatchlistItem = z.infer<typeof ExistingWatchlistItemSchema>;
