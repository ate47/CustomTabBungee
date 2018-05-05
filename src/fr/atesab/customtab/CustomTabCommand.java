package fr.atesab.customtab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.atesab.customtab.SubCommand.CommandType;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class CustomTabCommand extends Command implements TabExecutor {
	static List<String> getTabCompletion(List<String> options, String[] args) {
		if (options.size() == 0)
			return options;
		List<String> options_End = new ArrayList<String>();
		if (args.length == 0)
			return options_End;
		String start = args[args.length - 1].toLowerCase();
		for (int i = 0; i < options.size(); i++) {
			if (options.get(i).toLowerCase().startsWith(start.toLowerCase()))
				options_End.add(options.get(i));
		}
		options_End.sort((o1, o2) -> o1.compareToIgnoreCase(o2)); // sort by name
		return options_End;
	}

	static boolean hasPerm(CommandSender sender, String perm) {
		return perm != null ? sender.hasPermission(perm) : true;
	}

	private List<SubCommand> subCommands;
	private SCOpt opt;
	private SCShow show;
	private CustomTabPlugin plugin;

	public CustomTabCommand(CustomTabPlugin plugin) {
		super("gtab", "ctp.gtab", "globaltab");
		this.plugin = plugin;
		subCommands = new ArrayList<>();
		subCommands.add(show = new SCShow());
		subCommands.add(new SCSetLine(plugin));
		subCommands.add(new SCAddLine(plugin));
		subCommands.add(new SCDelLine(plugin));
		opt = new SCOpt(plugin);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		CommandType type = null;
		SubCommand subCommand;
		if (args.length < 1
				|| (type = args[0].equalsIgnoreCase("footer") || args[0].equalsIgnoreCase("f") ? CommandType.footer
						: (args[0].equalsIgnoreCase("header") || args[0].equalsIgnoreCase("h") ? CommandType.header
								: null)) == null
				|| args.length < 2 || (subCommand = getSubCommandByName(args[1])) == null
				|| !(hasPerm(sender, subCommand.getPermission()))) {
			if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				try {
					plugin.loadConfig();
					sender.sendMessage(new ComponentBuilder("Config reloaded.").color(ChatColor.GREEN).create());
				} catch (Exception e) {
					sender.sendMessage(new ComponentBuilder("An error occurred while saving config.")
							.color(ChatColor.RED).create());
					e.printStackTrace();
				}
				return;
			}
			boolean flag = args.length > 0 && args[0].equalsIgnoreCase(opt.getName());
			if (!flag && args.length > 0) {
				for (String alias : opt.getAliases())
					if (alias.equalsIgnoreCase(args[0])) {
						flag = true;
						break;
					}
			}
			if (!flag) {
				sender.sendMessage(new ComponentBuilder(CustomTabPlugin.CHANNEL_NAME).color(ChatColor.RED).bold(true)
						.append(":").color(ChatColor.DARK_GRAY).create());
				if (type != null) {
					show.execute(sender, new String[] {}, getName(), type);
				} else {
					sender.sendMessage(
							new ComponentBuilder("/" + getName() + " footer").color(ChatColor.GOLD).bold(true)
									.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + getName() + " footer"))
									.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
											new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
													.append("Click to show available footer options")
													.color(ChatColor.YELLOW).create()))
									.append(": ").color(ChatColor.GRAY).append("Show available footer options").reset()
									.create());
					sender.sendMessage(
							new ComponentBuilder("/" + getName() + " header").color(ChatColor.GOLD).bold(true)
									.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + getName() + " header"))
									.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
											new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
													.append("Click to show available header options")
													.color(ChatColor.YELLOW).create()))
									.append(": ").color(ChatColor.GRAY).append("Show available header options").reset()
									.create());
					if (hasPerm(sender, opt.getPermission()))
						sender.sendMessage(new ComponentBuilder("/" + getName() + " " + opt.getName())
								.color(ChatColor.GOLD).bold(true)
								.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
										"/" + getName() + " " + opt.getName()))
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
												.append("Click to " + opt.getDescription().toLowerCase())
												.color(ChatColor.YELLOW).create()))
								.append(": ").color(ChatColor.GRAY).append(opt.getDescription()).reset().create());
					if (hasPerm(sender, "ctp.gtab.reload"))
						sender.sendMessage(new ComponentBuilder("/" + getName() + " reload").color(ChatColor.GOLD)
								.bold(true)
								.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + getName() + " reload"))
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
												.append("Click to reload the plugin").color(ChatColor.YELLOW).create()))
								.append(": ").color(ChatColor.GRAY).append("Reload the plugin").reset().create());
				}
			} else {
				String[] sArgs = new String[args.length - 1];
				System.arraycopy(args, 1, sArgs, 0, sArgs.length);
				if (!opt.execute(sender, sArgs, getName(), null))
					sender.sendMessage(new ComponentBuilder("/" + getName() + " " + opt.getUsage(sender))
							.color(ChatColor.RED).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
									"/" + getName() + " " + opt.getName()))
							.create());
			}

		} else {
			String[] sArgs = new String[args.length - 2];
			System.arraycopy(args, 2, sArgs, 0, sArgs.length);
			if (!subCommand.execute(sender, sArgs, getName(), type))
				sender.sendMessage(
						new ComponentBuilder("/" + getName() + " " + " " + type.name() + subCommand.getUsage(sender))
								.color(ChatColor.RED).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
										"/" + getName() + " " + type.name() + " " + subCommand.getName()))
								.create());
		}
	}

	private SubCommand getSubCommandByName(String name) {
		for (SubCommand subCommand : subCommands)
			if (subCommand.getName().equalsIgnoreCase(name))
				return subCommand;
			else
				for (String alias : subCommand.getAliases())
					if (alias.equalsIgnoreCase(name))
						return subCommand;
		return null;
	}

	public String getUsage(CommandSender sender) {
		String s = "";
		for (SubCommand subCommand : subCommands)
			if (((hasPerm(sender, subCommand.getPermission()))))
				s += (s.isEmpty() ? "" : "|") + subCommand.getName();
		return getName() + " <footer|header" + (hasPerm(sender, opt.getPermission()) ? "|" + opt.getName() : "")
				+ (hasPerm(sender, "ctp.gtab.reload") ? "|reload" : "") + "> [" + s + "]";
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		ArrayList<String> list = new ArrayList<>();
		if (args.length > 0 && (args[0].equalsIgnoreCase("footer") || args[0].equalsIgnoreCase("header")
				|| args[0].equalsIgnoreCase("f") || args[0].equalsIgnoreCase("h"))) {
			if (args.length > 2) {
				SubCommand subCommand = getSubCommandByName(args[1]);
				if (subCommand != null && (hasPerm(sender, subCommand.getPermission()))) {
					String[] sArgs = new String[args.length - 2];
					System.arraycopy(args, 2, sArgs, 0, sArgs.length);
					List<String> subComplete = subCommand.onTabComplete(sender, args,
							args[0].equalsIgnoreCase("footer") || args[0].equalsIgnoreCase("f") ? CommandType.footer
									: (args[0].equalsIgnoreCase("header") || args[0].equalsIgnoreCase("h")
											? CommandType.header
											: null));
					if (subComplete != null)
						list.addAll(subComplete);
				}
			} else {
				subCommands.stream().filter(s -> (hasPerm(sender, s.getPermission()))).forEach(s -> {
					list.add(s.getName());
					list.addAll(Arrays.asList(s.getAliases()));
				});
			}
		} else {
			list.add("footer");
			list.add("f");
			list.add("header");
			list.add("h");
			if (hasPerm(sender, opt.getPermission())) {
				list.add(opt.getName());
				list.addAll(Arrays.asList(opt.getAliases()));
			}
			if (hasPerm(sender, "ctp.gtab.reload"))
				list.add("reload");
		}
		return getTabCompletion(list, args);
	}
}
