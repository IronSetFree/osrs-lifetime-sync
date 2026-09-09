package com.osrslifetime;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
    name = "OSRS Lifetime Sync",
    description = "Opt-in sync of Hans account age and time played to a Discord bot",
    tags = {"time", "played", "account", "age", "discord", "hans", "lifetime"},
    enabledByDefault = false
)
public class OsrsLifetimePlugin extends Plugin
{
    private static final int TIME_PLAYED_VARC = 526;
    private static final int TICKS_PER_MINUTE = 100;
    private static final int SYNC_EVERY_TICKS = 1000; // roughly 10 minutes

    private static final Pattern HANS_PATTERN = Pattern.compile(
        "(?i).*spent(?: here)?\\s+" +
        "(?:(\\d[\\d,]*)\\s+days?,\\s*)?" +
        "(?:(\\d[\\d,]*)\\s+hours?,\\s*)?" +
        "(\\d[\\d,]*)\\s+minutes?\\s+in the world since you arrived\\s+" +
        "(\\d[\\d,]*)\\s+days? ago.*"
    );

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configManager;
    @Inject private OsrsLifetimeConfig config;
    @Inject private OkHttpClient okHttpClient;
    @Inject private Gson gson;

    private final AtomicBoolean syncInFlight = new AtomicBoolean(false);
    private int playtimeMinutes = -1;
    private int minuteTicks = 0;
    private int ticksSinceSync = 0;
    private String currentRsn;

    @Provides
    OsrsLifetimeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OsrsLifetimeConfig.class);
    }

    @Override
    protected void startUp()
    {
        playtimeMinutes = config.lastPlaytimeMinutes();
    }

    @Override
    protected void shutDown()
    {
        savePlaytime();
        if (isLinked() && playtimeMinutes >= 0 && currentRsn != null)
        {
            submitSync(playtimeMinutes, null, false);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            if (client.getLocalPlayer() != null)
            {
                currentRsn = client.getLocalPlayer().getName();
            }
            int gameValue = client.getVarcIntValue(TIME_PLAYED_VARC);
            if (gameValue > playtimeMinutes)
            {
                playtimeMinutes = gameValue;
                minuteTicks = 0;
                savePlaytime();
            }
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.CONNECTION_LOST)
        {
            if (isLinked() && playtimeMinutes >= 0 && currentRsn != null)
            {
                submitSync(playtimeMinutes, null, false);
            }
            savePlaytime();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (client.getLocalPlayer() != null)
        {
            currentRsn = client.getLocalPlayer().getName();
        }

        minuteTicks++;
        ticksSinceSync++;

        if (minuteTicks >= TICKS_PER_MINUTE)
        {
            minuteTicks -= TICKS_PER_MINUTE;
            if (playtimeMinutes >= 0)
            {
                playtimeMinutes++;
            }

            int gameValue = client.getVarcIntValue(TIME_PLAYED_VARC);
            if (gameValue > playtimeMinutes)
            {
                playtimeMinutes = gameValue;
                minuteTicks = 0;
            }
        }

        if (ticksSinceSync >= SYNC_EVERY_TICKS)
        {
            ticksSinceSync = 0;
            savePlaytime();
            if (isLinked() && playtimeMinutes >= 0 && currentRsn != null)
            {
                submitSync(playtimeMinutes, null, false);
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.DIALOG)
        {
            return;
        }

        String raw = event.getMessage().replace("<br>", " ").replace("<br/>", " ");
        String text = Text.removeTags(raw).replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        Matcher matcher = HANS_PATTERN.matcher(text);
        if (!matcher.matches())
        {
            return;
        }

        int daysPlayed = parseNumber(matcher.group(1));
        int hoursPlayed = parseNumber(matcher.group(2));
        int minutesPlayed = parseNumber(matcher.group(3));
        int accountAgeDays = parseNumber(matcher.group(4));

        playtimeMinutes = daysPlayed * 1440 + hoursPlayed * 60 + minutesPlayed;
        minuteTicks = 0;
        ticksSinceSync = 0;
        savePlaytime();

        if (client.getLocalPlayer() != null)
        {
            currentRsn = client.getLocalPlayer().getName();
        }

        if (currentRsn == null)
        {
            notifyUser("Captured Hans data, but could not read your character name.");
            return;
        }

        if (!isLinked() && config.linkCode().trim().isEmpty())
        {
            notifyUser("Captured Hans data. Run /link in Discord and paste the code into this plugin, then ask Hans again.");
            return;
        }

        notifyUser("Captured Hans: " + playtimeMinutes + " minutes played, account age " + accountAgeDays + " days. Syncing...");
        submitSync(playtimeMinutes, accountAgeDays, true);
    }

    private int parseNumber(String value)
    {
        return value == null || value.isEmpty() ? 0 : Integer.parseInt(value.replace(",", ""));
    }

    private boolean isLinked()
    {
        return config.syncToken() != null && !config.syncToken().trim().isEmpty();
    }

    private void savePlaytime()
    {
        if (playtimeMinutes >= 0)
        {
            configManager.setConfiguration(OsrsLifetimeConfig.GROUP, "lastPlaytimeMinutes", playtimeMinutes);
        }
    }

    private void submitSync(int minutes, Integer accountAgeDays, boolean notifySuccess)
    {
        if (!syncInFlight.compareAndSet(false, true))
        {
            return;
        }

        String baseUrl = config.apiBaseUrl().trim().replaceAll("/+$", "");
        if (baseUrl.isEmpty())
        {
            syncInFlight.set(false);
            notifyUser("OSRS Lifetime API URL is empty.");
            return;
        }

        SyncPayload payload = new SyncPayload();
        payload.rsn = currentRsn;
        payload.playtimeMinutes = minutes;
        payload.accountAgeDays = accountAgeDays;

        String token = config.syncToken() == null ? "" : config.syncToken().trim();
        String linkCode = config.linkCode() == null ? "" : config.linkCode().trim();
        if (token.isEmpty())
        {
            if (linkCode.isEmpty())
            {
                syncInFlight.set(false);
                return;
            }
            payload.linkCode = linkCode;
        }

        Request.Builder requestBuilder = new Request.Builder()
            .url(baseUrl + "/api/v1/sync")
            .header("User-Agent", "RuneLite OSRS-Lifetime-Sync/0.1.0")
            .post(RequestBody.create(RuneLiteAPI.JSON, gson.toJson(payload)));

        if (!token.isEmpty())
        {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        okHttpClient.newCall(requestBuilder.build()).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                syncInFlight.set(false);
                log.warn("OSRS Lifetime sync failed", e);
                if (notifySuccess)
                {
                    notifyUser("OSRS Lifetime sync failed: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (Response closeable = response)
                {
                    String body = closeable.body() == null ? "" : closeable.body().string();
                    if (!closeable.isSuccessful())
                    {
                        log.warn("OSRS Lifetime sync returned {}: {}", closeable.code(), body);
                        notifyUser("OSRS Lifetime sync rejected (HTTP " + closeable.code() + "). " + body);
                        return;
                    }

                    SyncResponse syncResponse = gson.fromJson(body, SyncResponse.class);
                    if (syncResponse != null && syncResponse.syncToken != null && !syncResponse.syncToken.isEmpty())
                    {
                        configManager.setConfiguration(OsrsLifetimeConfig.GROUP, "syncToken", syncResponse.syncToken);
                        configManager.setConfiguration(OsrsLifetimeConfig.GROUP, "linkCode", "");
                        notifyUser("Linked successfully. Your Discord data starts private; use /visibility when you want it public.");
                    }
                    else if (notifySuccess)
                    {
                        notifyUser("OSRS Lifetime synced successfully.");
                    }
                }
                finally
                {
                    syncInFlight.set(false);
                }
            }
        });
    }

    private void notifyUser(String message)
    {
        clientThread.invokeLater(() -> client.addChatMessage(
            ChatMessageType.CONSOLE,
            "",
            "<col=00ff80>OSRS Lifetime:</col> " + message,
            null
        ));
    }

    private static class SyncPayload
    {
        String rsn;
        int playtimeMinutes;
        Integer accountAgeDays;
        String linkCode;
    }

    private static class SyncResponse
    {
        String syncToken;
    }
}
