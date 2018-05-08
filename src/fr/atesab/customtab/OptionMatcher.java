package fr.atesab.customtab;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class OptionMatcher {

	private Pattern pattern;
	private String usage;
	private BiFunction<ProxiedPlayer, Matcher, String> function;
	private BiFunction<ProxiedPlayer, OptionMatcher, String> exampleFunction;
	private boolean canBeTabbed;

	public OptionMatcher(Pattern pattern, String usage, BiFunction<ProxiedPlayer, Matcher, String> function,
			BiFunction<ProxiedPlayer, OptionMatcher, String> exampleFunction, boolean canBeTabbed) {
		this.pattern = pattern;
		this.usage = usage;
		this.function = function;
		this.exampleFunction = exampleFunction;
		this.canBeTabbed = canBeTabbed;
	}

	public boolean canBeTabbed() {
		return canBeTabbed;
	}

	public BiFunction<ProxiedPlayer, OptionMatcher, String> getExampleFunction() {
		return exampleFunction;
	}

	public BiFunction<ProxiedPlayer, Matcher, String> getFunction() {
		return function;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String getUsage() {
		return usage;
	}

	public void setFunction(BiFunction<ProxiedPlayer, Matcher, String> function) {
		this.function = function;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}
}
