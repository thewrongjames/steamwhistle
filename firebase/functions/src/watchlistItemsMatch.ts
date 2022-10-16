import {NewWatchlistItem} from "./NewWatchlistItem";

export function watchlistItemsMatch(
  firstItem: NewWatchlistItem,
  secondItem: NewWatchlistItem,
) {
  return firstItem.appId === secondItem.appId &&
    firstItem.threshold === secondItem.threshold;
}
