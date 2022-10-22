export interface PriceData {
  final: number,
  initial: number,
  discountPercentage: number,
}

export interface GamePriceInformation {
  isFree: boolean,
  priceData: PriceData
}

export interface Game extends GamePriceInformation {
  appId: number
  name: string
}
