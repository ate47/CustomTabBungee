package fr.atesab.customtab;

import java.util.List;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public abstract class SubCommand extends Command {
	public static String buildString(String[] array, int start) {
		return buildString(array, start, " ");
	}

	public static String buildString(String[] array, int start, String glue) {
		String s = "";
		for (int i = start; i < array.length; i++) {
			if (i > start)
				s += glue;
			s += array[i];
		}
		return s;
	}

	private String description;

	public SubCommand(String name, String permission, String description, String... aliases) {
		super(name, permission, aliases);
		this.description = description;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		execute(sender, args, "", null);
	}

	public abstract boolean execute(CommandSender sender, String[] args, String main, CommandType type);

	public String getDescription() {
		return description;
	}

	public String getUsage(CommandSender sender) {
		return getName();
	}

	public List<String> onTabComplete(CommandSender sender, String[] args, CommandType type) {
		return null;
	}

	enum CommandType {
		footer, header;
	}
}