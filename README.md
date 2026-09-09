# OSRS Lifetime Sync

RuneLite plugin for the OSRS Lifetime Discord bot.

The plugin lets a player explicitly link an Old School RuneScape character to Discord and sync the character's account age and time played. The Discord bot can then calculate and display statistics such as total playtime, approximate account creation date, average hours played per day, and the percentage of the account's lifetime spent logged in.

## How it works

1. Run `/link` in the OSRS Lifetime Discord bot.
2. Paste the one-time link code into the RuneLite plugin configuration.
3. Talk to Hans in Lumbridge and ask, `Can you tell me how long I've been here?`
4. The plugin reads Hans' account-age/playtime dialogue and performs the initial sync.
5. After linking, the plugin keeps playtime updated and periodically syncs while RuneLite is open.

New links are private by default. Visibility is controlled through the Discord bot.

## Production service

The plugin communicates with the OSRS Lifetime service at:

```text
https://osrs-lifetime-bot-production.up.railway.app
```

The server URL is fixed in the plugin and is not user-configurable.

## Data sharing and privacy

This plugin communicates with a third-party OSRS Lifetime server when you explicitly link your account and during later synchronization.

The plugin sends:

- your RuneScape display name (RSN),
- total playtime in minutes,
- the whole-number account age in days reported by Hans during initial linking,
- the one-time Discord link code during initial linking,
- a random synchronization token returned by the OSRS Lifetime server for later authenticated syncs.

The synchronization token is stored in RuneLite configuration as a hidden secret value. The plugin does **not** request or send your Jagex password, RuneScape password, bank PIN, authenticator code, or Jagex Launcher credentials.

The server derives an approximate creation date from Hans' whole-number account age. Because Hans reports whole days rather than an exact timestamp, the derived calendar date can be off by roughly one day.

## Development

Requirements:

- IntelliJ IDEA Community Edition
- Java 17 for the Gradle JVM
- Java 11-compatible compilation target

Open this repository as a Gradle project in IntelliJ. The project includes a Gradle task named:

```text
run
```

Running that task launches RuneLite in developer mode with `OSRS Lifetime Sync` loaded.

The plugin uses the production OSRS Lifetime server while running in development as well.

## RuneLite Plugin Hub

This project uses `build=standard` and intentionally adds no third-party Java dependencies beyond dependencies already supplied by RuneLite.

Because the plugin sends player data to the OSRS Lifetime service, the Plugin Hub manifest includes a third-party-server warning that identifies the data being sent.

## License

BSD 2-Clause License. See [LICENSE](LICENSE).
