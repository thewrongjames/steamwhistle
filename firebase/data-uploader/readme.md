# Data Uploader

This is just a small script to upload steam game data to firestore. This can be used to either bootstrap our application or to fill out an emulator.

The data file is just the first 1000 games from the first 10,000 games list from the mock database, and that is because of limitations on the reads and writes and sizes in the firestore and algolia free tiers. We might manually add in a couple more of our favourite / of the most popular games manually so that they show up too. We may also cut out more to keep the count down. The main point is that it is in that format. `has_more_results` and `last_appid` have been removed as they aren't really meaningful when the data has been manually messed with.

You will need to get a service key or secret or whatever it is from https://console.firebase.google.com/u/0/project/_/settings/serviceaccounts and save it in `secret.json` for this to work. Please don't run this on production too much though... we only have 20,000 free uploads. And uploading 1,000 at a time can make that go pretty quick.

You can run this uses the emulator by default, when run with `npm start`. You can (carefully) run it on the actual firebase using `npm start:live`, though I actually haven't tested that.