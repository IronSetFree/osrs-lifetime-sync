package com.osrslifetime;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OsrsLifetimePluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(OsrsLifetimePlugin.class);
        RuneLite.main(args);
    }
}
