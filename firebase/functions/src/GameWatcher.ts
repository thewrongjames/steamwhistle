import {z} from "zod";

export const GameWatcherSchema = z.object({
  uid: z.string(),
  threshold: z.number().nonnegative().int(),
});

export type GameWatcher = z.infer<typeof GameWatcherSchema>;
