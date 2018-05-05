package fr.atesab.customtab;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class SCOpt extends SubCommand {
	private CustomTabPlugin plugin;

	public SCOpt(CustomTabPlugin plugin) {
		super("opt", "ctp.gtab.opt", "Show available text options");
		this.plugin = plugin;
	}

	@Override
	public boolean execute(CommandSender sender, String[] args, String main, CommandType type) {
		if (args.length > 1)
			return false;
		int elementByPage = sender instanceof ProxiedPlayer ? 7 : 50;
		int maxPage = plugin.getTextOptions().size() / elementByPage
				+ (plugin.getTextOptions().size() % elementByPage != 0 ? 1 : 0);
		int page = 0;
		if (args.length == 1
				&& !(args[0].matches("[0-9]+") && (page = Integer.valueOf(args[0]) - 1) >= 0 && page < maxPage)) {
			sender.sendMessage(new ComponentBuilder("This is not a valid page").color(ChatColor.RED).create());
			return true;
		}
		ComponentBuilder title = new ComponentBuilder("Options").bold(true).color(ChatColor.RED);
		if (maxPage != 1)
			title.append(" (").reset().color(ChatColor.DARK_GRAY).append(String.valueOf(page + 1)).color(ChatColor.AQUA)
					.append("/").color(ChatColor.DARK_GRAY).append(String.valueOf(maxPage)).color(ChatColor.AQUA)
					.append(")").color(ChatColor.DARK_GRAY);
		sender.sendMessage(title.append(": ").bold(true).color(ChatColor.RED).create());
		List<String> keys = new ArrayList<>(plugin.getTextOptions().keySet());
		for (int i = page * elementByPage; i < keys.size() && i < (page + 1) * elementByPage; i++) {
			String k = keys.get(i);
			ComponentBuilder msg = new ComponentBuilder("- ").color(ChatColor.GRAY).append("%" + k + "%")
					.color(ChatColor.GOLD);
			if (sender instanceof ProxiedPlayer)
				msg.append(": ").color(ChatColor.GRAY)
						.append(plugin.getTextOptions().get(k).apply(((ProxiedPlayer) sender))).color(ChatColor.WHITE);
			sender.sendMessage(msg.create());
		}
		if (sender instanceof ProxiedPlayer && maxPage != 1) {
			ComponentBuilder end = new ComponentBuilder("<--").reset();
			if (page > 0)
				end.bold(true).color(ChatColor.YELLOW)
						.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + main + " " + getName() + " " + page))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
										.append("Last page").reset().bold(true).color(ChatColor.YELLOW).create()));
			else
				end.color(ChatColor.DARK_GRAY);
			end.append(" | ").reset().color(ChatColor.DARK_GRAY).append("-->");
			if (page + 1 < maxPage)
				end.bold(true).color(ChatColor.YELLOW)
						.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
								"/" + main + " " + getName() + " " + (page + 2)))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
										.append("Next page").color(ChatColor.YELLOW).create()));
			else
				end.color(ChatColor.DARK_GRAY);
			sender.sendMessage(end.create());
		}
		return true;
	}

	@Override
	public String getUsage(CommandSender sender) {
		return super.getUsage(sender) + " [page]";
	}

}
