import { getDoc, setDoc } from "firebase/firestore"

import { createTestEnvironment } from './createTestEnvironment.js'
import { test } from './test.js'

const testEnvironment = await createTestEnvironment()

const alicesContext = testEnvironment.authenticatedContext("alice")
const alicesFirestore = alicesContext.firestore()
const notLoggedInContext = testEnvironment.unauthenticatedContext()
const notLoggedInFirestore = notLoggedInContext.firestore()

await test(
  'An authenticated user cannot write to their user document',
  setDoc(alicesFirestore.doc('/users/alice'), {}),
  false,
)

await test(
  'An authenticated user can make a valid document in their watchlist',
  setDoc(alicesFirestore.doc('/users/alice/watchlist/42'), {
    appId: 42,
    threshold: 4000
  }),
  true,
)

await test(
  'An authenticated user can make a valid update to their watchlist',
  setDoc(alicesFirestore.doc('/users/alice/watchlist/42'), {
    appId: 42,
    threshold: 2000
  }),
  true,
)

await test(
  'An authenticated user cannot write an invalid document to their watchlist 1',
  setDoc(alicesFirestore.doc('/users/alice/watchlist/42'), {
    appId: 42
  }),
  false,
)

await test(
  'An authenticated user cannot write an invalid document to their watchlist 2',
  setDoc(alicesFirestore.doc('/users/alice/watchlist/42'), {
    threshold: 4000
  }),
  false,
)

await test(
  'An authenticated user cannot write an invalid document to their watchlist 3',
  setDoc(alicesFirestore.doc('/users/alice/watchlist/42'), {
    appId: 42,
    threshold: 4000,
    somethingElse: 6
  }),
  false,
)

await test(
  'An authenticated user cannot write an invalid document to their watchlist 4',
  setDoc(alicesFirestore.doc('/users/alice/watchlist/42'), {
    appId: 42,
    threshold: '4000',
  }),
  false,
)

await test(
  'An authenticated user cannot write an invalid document to their watchlist 5',
  setDoc(alicesFirestore.doc('/users/alice/watchlist/42'), {
    appId: '42',
    threshold: 4000,
  }),
  false,
)

await test(
  'An authenticated user cannot write to a game watchlist',
  setDoc(alicesFirestore.doc('/games/42/watchers/alice'), {
    appId: '42',
    threshold: 4000,
  }),
  false,
)

await test(
  'An unathenticated user cannot write a valid document to a watchlist',
  setDoc(notLoggedInFirestore.doc('/users/bob/watchlist/24'), {
    a: 2,
    b: 3
  }),
  false,
)

await test(
  'An unathenticated user cannot read a watchlist item',
  getDoc(notLoggedInFirestore.doc('/users/alice/watchlist/42')),
  false,
)

// Do the final cleanup of the testing environment.
await testEnvironment.cleanup()
