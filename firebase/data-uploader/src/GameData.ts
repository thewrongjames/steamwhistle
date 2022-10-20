import {z} from "zod";

export const GameDataSchema = z.object({
  response: z.object({
    apps: z.array(z.object({
      appid: z.number().int().positive(),
      name: z.string(),
      last_modified: z.number().int(),
      price_change_number: z.number().int(),
    })),
  }),
});

export type GameData = z.infer<typeof GameDataSchema>
