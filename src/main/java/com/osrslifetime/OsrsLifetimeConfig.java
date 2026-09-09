package com.osrslifetime;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OsrsLifetimeConfig.GROUP)
public interface OsrsLifetimeConfig extends Config
{
    String GROUP = "osrslifetime";

    @ConfigItem(
        keyName = "linkCode",
        name = "Discord link code",
        description = "Run /link in Discord, paste the one-time code here, then ask Hans how long you've been here.",
        position = 0
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
