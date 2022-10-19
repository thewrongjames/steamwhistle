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

I didn't expect this to get complicated enough to warrant using a testing framework, but in retrospect it probably has. I don't think I'm going to refactor this now, but if we end up really want to I can.

## Dependencies

You will need to run `npm install` in both the `functions` and `firestore-tests` directory to use them.

## Linting

If you edit code in their `functions` or `firestore-tests` you should ensure that running `npm lint` in either directory does not bring up any errors or warnings.

Both `functions` and `firestore-tests` use eslint. They use the same config because I just copied from `functions` when I made `firestore-tests`. This is the default linting config the firebase gives you, so it seems reasonable to stick with that.

If you are using vscode, you can have highlighting for linting issues. I haved committed a `.vscode` directory with a setting that allows vscode to have the two projects working side by side. Without that it complains about not being able to find the `tsconfig` files because it looks at the top level of the open project (and if you make the paths in `eslintrc` relative to the `steamwhistle` directory, then when you run `npm lint` it can't find them because that works relative to the npm project directories).

At the moment there is decent duplication between the eslint settings. In principle this should be abstracted out into a shared config file that sits outside of either project (see [the extending configuration files docs](https://eslint.org/docs/latest/user-guide/configuring/configuration-files#extending-configuration-files)), but, that would mean that the `.eslintrc` files themselves would not be linted, which maybe isn't important but was enough to motivate me to not put the effort into abstracting them at the moment.