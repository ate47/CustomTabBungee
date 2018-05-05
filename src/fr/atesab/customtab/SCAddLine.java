package fr.atesab.customtab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class SCAddLine extends SubCommand {
	private CustomTabPlugin plugin;

	public SCAddLine(CustomTabPlugin plugin) {
		super("addline", "ctp.gtab.addline", "Add a line", "a");
		this.plugin = plugin;
	}

	@Override
	public boolean execute(CommandSender sender, String[] args, String main, CommandType type) {
		if (args.length < 2 || !args[0].matches("[0-9]+"))
			return false;
		int line = Integer.valueOf(args[0]).intValue() - 1;
		String[] lines = (type == CommandType.footer ? CustomTabPlugin.getGlobalFooter()
				: type == CommandType.header ? CustomTabPlugin.getGlobalHeader() : "").split("\n");
		if (line >= 0 && line <= lines.length) {
			String[] newLines = new String[lines.length + 1];
			System.arraycopy(lines, 0, newLines, 0, line);
			System.arraycopy(lines, line, newLines, line + 1, lines.length - line);
			newLines[line] = buildString(args, 1);
			switch (type) {
			case footer:
				plugin.setGlobalFooter(buildString(newLines, 0, "\n"));
				break;
			case header:
				plugin.setGlobalHeader(buildString(newLines, 0, "\n"));
				break;
			}
			try {
				plugin.saveConfig();
				sender.sendMessage(new ComponentBuilder("Line ").color(ChatColor.GREEN).append(String.valueOf(line + 1))
						.color(ChatColor.YELLOW).append(" added.").color(ChatColor.GREEN).create());
			} catch (IOException e) {
				sender.sendMessage(
						new ComponentBuilder("An error occurred while saving config.").color(ChatColor.RED).create());
				e.printStackTrace();
			}
		} else
			sender.sendMessage(new ComponentBuilder("This is not a valid line.").color(ChatColor.RED).create());
		return true;
	}

	@Override
	public String getUsage(CommandSender sender) {
		return super.getUsage(sender) + " <line> <text>";
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String[] args, CommandType type) {
		List<String> list = new ArrayList<>();
		plugin.getTextOptions().keySet().forEach(k -> list.add("%" + k + "%"));
		return list;
	}
}
