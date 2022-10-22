import {z} from "zod";

export const GameWatcherSchema = z.object({
  uid: z.string(),
  threshold: z.number().int(),
});

export type GameWatcher = z.infer<typeof GameWatcherSchema>;
