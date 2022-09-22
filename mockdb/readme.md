# Quick README

The files in this subfolder contains actual JSON response payloads for the API calls outlined on the wiki.

Refer to the list below to determine which API call each file corresponds to, and any additional notes if any.

For more information on the API itself, refer to the Wiki link: `https://github.com/thewrongjames/steamwhistle/wiki/Steam-API`

## Reference List

### `get_all_apps.json`

Endpoint mocked: `http://api.steampowered.com/ISteamApps/GetAppList/v0002`

Description: List of all apps currently available on Steam store.

Notes: No filtering done.

### `get_app_discounted_horizon_zero_dawn.json`

Endpoint mocked: `http://store.steampowered.com/api/appdetails?appids=1151640`

Description: Example of a video game currently on sale.

Notes: Includes a deluxe edition also on sale.

### `get_app_f2p_dota2.json`

Endpoint mocked: `http://store.steampowered.com/api/appdetails?appids=570`

Description: Example of a video game that is free to play.

Notes: N/A

### `get_app_ibb_obb.json`

Endpoint mocked: `http://store.steampowered.com/api/appdetails?appids=95400`

Description: Example of a video game with information populated.

Notes: Unlike the other calls, this one is pretty ordinary - probably what you'd expect for an average game not on sale.

### `get_app_preorder_crisis_core.json`

Endpoint mocked: `http://store.steampowered.com/api/appdetails?appids=1608070`

Description: Example of a video game that is available for pre-order but not for general availability.

Notes: N/A

### `get_app_sc4_dlc.json`

Endpoint mocked: `http://store.steampowered.com/api/appdetails?appids=874343`

Description: Example of downloadable content for a core game.

Notes: N/A

### `get_app_software_rpg_maker_vx.json`

Endpoint mocked: `http://store.steampowered.com/api/appdetails?appids=521880`

Description: Example of a non-game application available for purchase.

Notes: N/A

### `get_app_video_episode_true_sight_2018.json`

Endpoint mocked: `http://store.steampowered.com/api/appdetails?appids=1017170`

Description: Example of a video **episode** available for download and viewing.

Notes: Not the video series itself, just one episode.

### `get_app_video_series_true_sight.json`

Endpoint mocked: `http://store.steampowered.com/api/appdetails?appids=539730`

Description: Example of a video series available for browsing/viewing.

Notes: The parent video series of the episode from the above example.

### `get_games_only_10k_p1.json` -> `get_games_only_10k_p8.json`

Endpoints mocked:

e.g. `https://api.steampowered.com/IStoreService/GetAppList/v1/?key={{SteamKey}}&include_games=true&include_dlc=false&include_software=false&include_videos=false&include_hardware=false&max_results=10000` \
and \
`https://api.steampowered.com/IStoreService/GetAppList/v1/?key={{SteamKey}}&include_games=true&include_dlc=false&include_software=false&include_videos=false&include_hardware=false&last_appid=496120&max_results=10000`

Description: JSON payloads broken up into arrays of up to 10000 app entries each. Requires a Steam Web API key.

Notes: Filters: games only, no dlc, no software, no videos and no hardware. Pagination continues based on `last_appid` of the previous file.

### `get_games_only_50k_p1.json` and `get_games_only_50k_p2.json`

Endpoints mocked:

e.g. `https://api.steampowered.com/IStoreService/GetAppList/v1/?key={{SteamKey}}&include_games=true&include_dlc=false&include_software=false&include_videos=false&include_hardware=false&max_results=50000` \
and \
`https://api.steampowered.com/IStoreService/GetAppList/v1/?key={{SteamKey}}&include_games=true&include_dlc=false&include_software=false&include_videos=false&include_hardware=false&last_appid=1512940&max_results=50000`

Description: As above but JSON payloads broken up into arrays of up to 50000 app entries each instead. Requires a Steam Web API key.

Notes: Filters: games only, no dlc, no software, no videos and no hardware. Pagination continues based on `last_appid` of the previous file.

### `get_user_wishlist.json`

Endpoint mocked: `https://store.steampowered.com/wishlist/profiles/76561198142358795/wishlistdata/`

Description: A user's wishlist retrieved successfully.

Notes: References the same user as the example below.

### `get_user_wishlist_by_vanity_url.json`

Endpoint mocked: `https://store.steampowered.com/wishlist/profiles/Abadon/wishlistdata/`

Description: A user's wishlist retrieved unsuccessfully using the vanity URL.

Notes: References the same user as the example above.

### `get_user_wishlist_private.json`

Endpoint mocked: `https://store.steampowered.com/wishlist/profiles/76561198034031910/wishlistdata/`

Description: A user's wishlist retrieved unsuccessfully due to privacy settings.

Notes: N/A
