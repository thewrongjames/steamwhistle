# SteamWhistle Firebase

## Emulators

Start the firebase emulators with:

```
cd firebase
firebase emulators:start
```

## Functions

Watch the functions typescript code and rebuild on any changes with:

```
cd firebase/functions
npm run build:watch
```

If you are running the emulators, they will in turn restart when these rebuild.

## Firestore Tests

There is also a `firestore-tests` directory, which houses code to test the firestore security rules. You will need a somewhat new version of node (16 and later should work, I'm not sure about earlier) to run these, as I am using some new JavaScript features. You will also need the emulators running for these tests to work. You can set them running (and recompiling and rerunning on changes) with:

```
cd firebase/firestore-tests
npm start
```

## Dependencies

You will need to run `npm install` in both the `functions` and `firestore-tests` directory to use them.