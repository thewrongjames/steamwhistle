import {WatchlistItem} from "../models/WatchlistItem";

export function watchlistItemsMatch(
  firstItem: WatchlistItem,
  secondItem: WatchlistItem
) {
  return (
    firstItem.appId === secondItem.appId &&
    firstItem.threshold === secondItem.threshold
  );
}
