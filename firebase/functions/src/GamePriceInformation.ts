export interface GamePriceInformation {
  isFree: boolean,
  priceData: {
    final: number,
    initial: number,
    discountPercentage: number,
  }
}
