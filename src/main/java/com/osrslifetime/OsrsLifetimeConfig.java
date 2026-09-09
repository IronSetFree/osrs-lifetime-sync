package com.osrslifetime;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OsrsLifetimeConfig.GROUP)
public interface OsrsLifetimeConfig extends Config
{
    String GROUP = "osrslifetime";

    @ConfigItem(
        keyName = "apiBaseUrl",
        name = "API URL",
        description = "Server used for OSRS Lifetime sync. When linked, the plugin sends your RSN, playtime, account age, and sync data to this server.",
        position = 0
    )
    default String apiBaseUrl()
    {
        return "http://localhost:3000";
    }

    @ConfigItem(
        keyName = "linkCode",
        name = "Discord link code",
        description = "Run /link in Discord, paste the one-time code here, then ask Hans how long you've been here.",
        position = 1
    )
    default String linkCode()
    {
        return "";
    }

    @ConfigItem(
        keyName = "syncToken",
        name = "",
        description = "",
        hidden = true,
        secret = true
    )
    default String syncToken()
    {
        return "";
    }

    @ConfigItem(
        keyName = "lastPlaytimeMinutes",
        name = "",
        description = "",
        hidden = true
    )
    default int lastPlaytimeMinutes()
    {
        return -1;
    }
}
