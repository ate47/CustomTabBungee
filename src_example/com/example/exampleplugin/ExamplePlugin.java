package com.example.exampleplugin;

import java.math.BigInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.atesab.customtab.CustomTabPlugin;
import fr.atesab.customtab.OptionMatcher;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class ExamplePlugin extends Plugin {
	@Override
	public void onEnable() {
		// Search CustomTabPlugin
		if (getProxy().getPluginManager().getPlugin("CustomTab") != null) {
			// Register a new text option with the name "myOption" (usable with %myOption%)

			CustomTabPlugin.registerTextOption("myOption",
					p -> p.isForgeUser() ? "You are a forge user" : "You are not a forge user");

			// an advanced option usable with %byteformat-<a integer>% / for example:
			// %byteformat-42000% = 42kB

			// static data
			char[] byteFormat = { 'k', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y' };
			BigInteger th = BigInteger.valueOf(1000L);

			CustomTabPlugin.registerTextOption(Pattern.compile("%byteformat-([0-9]+)%"), // pattern to match with
																							// text
					"%byteformat-<number>%", // usage of this pattern
					(p, m) -> {
						BigInteger value = new BigInteger(m.group(1)); // get the first group of the matched text
						// a simple method to get unit of a number
						int i;
						for (i = 0; value.compareTo(th) != -1 && i < byteFormat.length; i++)
							value = value.divide(th);
						return value.toString() + (i == 0 ? "" : byteFormat[i - 1]) + "B";
					}, (p, om) -> {
						return "%byteformat-42000% = 42kB"; // return an example of the function for the tab opt command
					}, false); // if the usage can be use in the tab command with [TAB]

			// The same text options in a other code style

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

			CustomTabPlugin.registerTextOption(Pattern.compile("%byteformat-([0-9]+)%"), "%byteformat-<number>%",
					new BiFunction<ProxiedPlayer, Matcher, String>() {
						@Override
						public String apply(ProxiedPlayer p, Matcher m) {
							BigInteger value = new BigInteger(m.group(1));
							int i;
							for (i = 0; value.compareTo(th) != -1 && i < byteFormat.length; i++) {
								value = value.divide(th);
							}
							if (i == 0) {
								return value.toString() + "B";
							} else {
								return value.toString() + byteFormat[i - 1] + "B";
							}
						}
					}, new BiFunction<ProxiedPlayer, OptionMatcher, String>() {
						@Override
						public String apply(ProxiedPlayer p, OptionMatcher om) {
							return "%byteformat-42000% = 42kB";
						}
					}, false);
		} else
			System.err.println("Custom tab not installed");
		super.onEnable();
	}
}
