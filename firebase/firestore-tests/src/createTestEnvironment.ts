import {
  initializeTestEnvironment,
  RulesTestEnvironment
} from '@firebase/rules-unit-testing'
import { setDoc } from "firebase/firestore"

import { WatchlistItem } from './WatchlistItem.js'

export async function createTestEnvironment(): Promise<RulesTestEnvironment> {
  const testEnvironment = await initializeTestEnvironment({
    projectId: 'steamwhistlemobile',
    firestore: {
      host: 'localhost',
      port: 8080,
    }
  })

  // Setup the documents in the testing environment, bypassing security rules.
  await testEnvironment.withSecurityRulesDisabled(async context => {
    const noRulesFirestore = context.firestore()

    await setDoc(noRulesFirestore.doc('/games/42'), {})
    await setDoc(noRulesFirestore.doc('/games/24'), {})

    const watchlistItem: WatchlistItem = {
      appId: 24,
      threshold: 2000,
    }

    await setDoc(
      noRulesFirestore.doc('/users/alice/watchlist/24'),
      watchlistItem
    )
    await setDoc(
      noRulesFirestore.doc('/games/24/watchers/alice'),
      watchlistItem
    )
  })

  return testEnvironment
}