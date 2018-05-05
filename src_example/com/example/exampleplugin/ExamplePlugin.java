package com.example.exampleplugin;

import java.util.function.Function;

import fr.atesab.customtab.CustomTabPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class ExamplePlugin extends Plugin {
	@Override
	public void onEnable() {
		// Search CustomTabPlugin
		if(getProxy().getPluginManager().getPlugin("CustomTab")!=null) {
			// Register a new text option with the name "myOption" (usable with %myOption%)
			
			CustomTabPlugin.registerTextOption("myOption",
					p -> p.isForgeUser() ? "You are a forge user" : "You are not a forge user");
			
			// The same text option in a other code style
			CustomTabPlugin.registerTextOption("myOption", new Function<ProxiedPlayer, String>() {
				@Override
				public String apply(ProxiedPlayer p) {
					if (p.isForgeUser()) {
						return "You are a forge user";
					} else {
						return "You are not a forge user";
					}
				}
			});
		} else
			System.err.println("Custom tab not installed");
		super.onEnable();
	}
}