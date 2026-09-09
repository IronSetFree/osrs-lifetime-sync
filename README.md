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

## Data sharing and privacy

This plugin communicates with a third-party OSRS Lifetime server when you explicitly link your account.

The plugin may send:

- your RuneScape display name (RSN),
- total playtime in minutes,
- the whole-number account age in days reported by Hans during initial linking,
- the one-time Discord link code during initial linking,
- a random synchronization token returned by the OSRS Lifetime server for later authenticated syncs.

The synchronization token is stored in RuneLite configuration as a secret value. The plugin does **not** request or send your Jagex password, RuneScape password, bank PIN, authenticator code, or Jagex Launcher credentials.

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

The development API URL defaults to:

```text
http://localhost:3000
```

For remote testing, set the plugin's **API URL** configuration to the HTTPS URL of your OSRS Lifetime server.

## RuneLite Plugin Hub

This repository is intended to be submitted to the RuneLite Plugin Hub using `build=standard`. It intentionally does not require third-party Java dependencies beyond dependencies already supplied by RuneLite.

Before public Plugin Hub release, the plugin must use a stable production HTTPS API endpoint and the Plugin Hub manifest should include a third-party-server warning describing the data listed above.

## License

BSD 2-Clause License. See [LICENSE](LICENSE).
