import {getDoc, getDocs, query, setDoc, Timestamp} from "firebase/firestore";

import {createTestEnvironment} from "./createTestEnvironment.js";
import {logStats, test} from "./test.js";

const testEnvironment = await createTestEnvironment();

const alicesContext = testEnvironment.authenticatedContext("alice");
const alicesFirestore = alicesContext.firestore();
const notLoggedInContext = testEnvironment.unauthenticatedContext();
const notLoggedInFirestore = notLoggedInContext.firestore();

await test(
  "An authenticated user cannot make an invalid write to their user document " +
  "(1)",
  setDoc(alicesFirestore.doc("/users/alice"), {}),
  false,
);

await test(
  "An authenticated user cannot make an invalid write to their user document " +
  "(2)",
  setDoc(alicesFirestore.doc("/users/alice"), {something: "Invalid"}),
  false,
);

await test(
  "An authenticated user cannot make an invalid write to their user document " +
  "(3)",
  setDoc(alicesFirestore.doc("/users/alice"), {uid: "bob"}),
  false,
);

await test(
  "An authenticated user can make a valid write to their user document",
  setDoc(alicesFirestore.doc("/users/alice"), {uid: "alice"}),
  true,
);

await test(
  "An unauthenticated user cannot make a write to a user document",
  setDoc(notLoggedInFirestore.doc("/users/alice"), {uid: "alice"}),
  false,
);

await test(
  "An authenticated user cannot make an invalid write to their device list (1)",
  setDoc(alicesFirestore.doc("/users/alice/devices/device"), {}),
  false,
);

await test(
  "An authenticated user cannot make an invalid write to their device list (2)",
  setDoc(alicesFirestore.doc("/users/alice/devices/device"), {
    something: "invalid",
  }),
  false,
);

await test(
  "An authenticated user cannot make an invalid write to their device list (3)",
  setDoc(alicesFirestore.doc("/users/alice/devices/device"), {
    devid: "Not the actual devid",
  }),
  false,
);

await test(
  "An authenticated user can make a valid write to their device list",
  setDoc(alicesFirestore.doc("/users/alice/devices/device"), {
    devid: "device",
  }),
  true,
);


await test(
  "An authenticated user can read a device of theirs",
  getDoc(alicesFirestore.doc("/users/alice/devices/1")),
  true,
);

await test(
  "An unauthenticated user cannot read a users device",
  getDoc(notLoggedInFirestore.doc("/users/alice/devices/1")),
  false,
);

await test(
  "An authenticated user cannot read a different user's device",
  getDoc(alicesFirestore.doc("/users/bob/devices/2")),
  false,
);

await test(
  "An authenticated user can make a valid document in their watchlist (1)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 42,
    threshold: 4000,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: true,
  }),
  true,
);

await test(
  "An authenticated user can make a valid document in their watchlist (2)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/24"), {
    appId: 24,
    threshold: 11,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: false,
  }),
  true,
);

await test(
  "An unauthenticated user cannot make a valid document in a watchlist",
  setDoc(notLoggedInFirestore.doc("/users/alice/watchlist/42"), {
    appId: 42,
    threshold: 2000,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: false,
  }),
  false,
);

await test(
  "An authenticated user can make a valid update to their watchlist",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 42,
    threshold: 2000,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: false,
  }),
  true,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (1)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 42,
    created: Timestamp.now(),
    isActive: false,
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (2)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    threshold: 4000,
    updated: Timestamp.now(),
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (3)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 42,
    threshold: 4000,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: false,
    somethingElse: 6,
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (4)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 42,
    threshold: "4000",
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: false,
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (5)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: "42",
    threshold: 4000,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: false,
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (6)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 42.1,
    threshold: 4000,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: false,
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (7)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 0.7895678678,
    threshold: -12.46734563567,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: false,
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (8)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 42,
    threshold: 1000,
    created: 12345678,
    updated: Timestamp.now(),
    isActive: false,
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (8)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 24,
    threshold: 1000,
    created: Timestamp.now(),
    updated: "87654321",
    isActive: false,
  }),
  false,
);

await test(
  "An authenticated user cannot write an invalid document to their " +
  "watchlist (9)",
  setDoc(alicesFirestore.doc("/users/alice/watchlist/42"), {
    appId: 24,
    threshold: 1000,
    created: Timestamp.now(),
    updated: Timestamp.now(),
    isActive: 1,
  }),
  false,
);

await test(
  "An authenticated user cannot write to a game watchlist",
  setDoc(alicesFirestore.doc("/games/42/watchers/alice"), {
    appId: 42,
    threshold: 4000,
  }),
  false,
);

await test(
  "An unathenticated user cannot read a watchlist item",
  getDoc(notLoggedInFirestore.doc("/users/alice/watchlist/42")),
  false,
);

await test(
  "An authenticated user can read an item on their watchlist",
  getDoc(alicesFirestore.doc("/users/alice/watchlist/42")),
  true,
);

await test(
  "An authenticated user cannot read an item on another user's watchlist",
  getDoc(alicesFirestore.doc("/users/bob/watchlist/24")),
  false,
);

await test(
  "An authenticated user can read their watchlist",
  getDocs(query(alicesFirestore.collection("/users/alice/watchlist"))),
  true,
);

await test(
  "An unauthenticated user cannot read a watchlist",
  getDocs(query(notLoggedInFirestore.collection("/users/alice/watchlist"))),
  false,
);

await test(
  "An authenticated user can list all games",
  getDocs(query(alicesFirestore.collection("/games"))),
  true,
);

await test(
  "An unauthenticated user cannot list all games",
  getDocs(query(notLoggedInFirestore.collection("/games"))),
  false,
);


// Do the final cleanup of the testing environment.
await testEnvironment.cleanup();
logStats();
