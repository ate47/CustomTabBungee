package fr.atesab.customtab;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.GsonBuilder;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class CustomTabPlugin extends Plugin implements Listener {
	private static String globalFooter = "";
	private static String globalHeader = "";
	private static boolean noBukkit = true;
	/**
	 * Channel used by CustomTab to send/receive Bukkit tab
	 * 
	 * @since 1.1
	 */
	public static final String CHANNEL_NAME = "CustomTab";
	/**
	 * Normal option name regex pattern to check if a normal text option has a valid
	 * name
	 * 
	 * @since 1.3
	 */
	public static final String NORMAL_OPTION_NAME_PATTERN = "[A-Za-z0-9\\_]*";
	private static List<OptionMatcher> textOptions = new ArrayList<>();
	/**
	 * Rate between every tab change in millisecond
	 * 
	 * @since 1.1
	 */
	public static final long REFRESH_RATE = 1000L;

	private static <T> ArrayList<T> asArrayList(Iterable<T> iterable) {
		ArrayList<T> arrayList = new ArrayList<>();
		iterable.forEach(arrayList::add);
		return arrayList;
	}

	private static File createDir(File dir) {
		dir.mkdirs();
		return dir;
	}

	private static File createFile(File file, String defaultContent) throws IOException {
		if (!file.exists()) {
			BufferedWriter bw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
			bw.write(defaultContent);
			bw.close();
		}
		return file;
	}

	/**
	 * get the global raw text footer
	 * 
	 * @return raw footer
	 * @since 1.2
	 */
	public static String getGlobalFooter() {
		return globalFooter;
	}

	/**
	 * get the global raw text header
	 * 
	 * @return raw header
	 * @since 1.2
	 */
	public static String getGlobalHeader() {
		return globalHeader;
	}

	/**
	 * Replace in a raw text the Bungee text options
	 * 
	 * @param raw
	 *            raw text data
	 * @param player
	 *            player to base information
	 * @return the text with options
	 * @since 1.1
	 * @see #registerTextOption(Pattern, String, BiFunction, BiFunction, boolean) to
	 *      register new options
	 */
	public static String getOptionnedText(String raw, ProxiedPlayer player) {
		for (OptionMatcher option : textOptions) {
			Matcher matcher = option.getPattern().matcher(raw);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				String result;
				try {
					result = (option.getFunction().apply(player, matcher));
				} catch (Exception e) {
					e.printStackTrace();
					result = "*error*";
				}
				matcher.appendReplacement(buffer, result);
			}
			matcher.appendTail(buffer);
			raw = buffer.toString();
		}
		return raw.replace('&', ChatColor.COLOR_CHAR);
	}

	static long getSystemTimeInSecond() {
		return System.currentTimeMillis() / 1000L;
	}

	/**
	 * @return if CustomTab ask to a Bukkit server or not local information
	 * @since 1.2
	 */
	public static boolean isBukkit() {
		return !noBukkit;
	}

	private static String loadFile(File file) throws IOException {
		String s = "";
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
		String line;
		while ((line = br.readLine()) != null)
			s += (s.isEmpty() ? "" : "\n") + line;
		br.close();
		return s;
	}

	/**
	 * Register a new text option for this plugin
	 * 
	 * @param optionMatcher
	 *            option matcher to match and evaluate the text
	 * @see #registerTextOption(OptionMatcher)
	 * @since 1.3
	 */
	public static void registerTextOption(OptionMatcher optionMatcher) {
		textOptions.removeIf(op -> op.getPattern().toString().equals(optionMatcher.getPattern().toString()));
		textOptions.add(optionMatcher);
	}

	/**
	 * Register a new text option for this plugin
	 * 
	 * @param pattern
	 *            pattern to match the option
	 * @param usage
	 *            the usage of this pattern
	 * @param option
	 *            text option associated
	 * @since 1.3
	 * @see #registerTextOption(Pattern, String, BiFunction, BiFunction, boolean)
	 */
	public static void registerTextOption(Pattern pattern, String usage,
			BiFunction<ProxiedPlayer, Matcher, String> option) {
		registerTextOption(pattern, usage, option, null, false);
	}

	/**
	 * Register a new text option for this plugin
	 * 
	 * @param pattern
	 *            pattern to match the option
	 * @param usage
	 *            the usage of this pattern
	 * @param option
	 *            text option associated
	 * @param exampleFunction
	 *            the example to show in the tab command opt list
	 * @param canBeTabbed
	 *            if in the tab command the option usage can be get with tab
	 * @since 1.3
	 */

	public static void registerTextOption(Pattern pattern, String usage,
			BiFunction<ProxiedPlayer, Matcher, String> option,
			BiFunction<ProxiedPlayer, OptionMatcher, String> exampleFunction, boolean canBeTabbed) {
		registerTextOption(new OptionMatcher(pattern, usage, option, exampleFunction, canBeTabbed));
	}

	/**
	 * Register a new text option for this plugin
	 * 
	 * @param name
	 *            name of the option to add
	 * @param option
	 *            text option associated with the specified name
	 * @throws IllegalArgumentException
	 *             if the name does contain non-alphanumerics characters
	 * @since 1.1
	 * @see #registerTextOption(Pattern, String, BiFunction)
	 */
	public static void registerTextOption(String name, Function<ProxiedPlayer, String> option) {
		if (!name.matches(NORMAL_OPTION_NAME_PATTERN))
			throw new IllegalArgumentException("name can only contain alphanumerics characters");
		String usage = "%" + name + "%";
		registerTextOption(Pattern.compile(usage), usage, (p, m) -> option.apply(p),
				(p, om) -> om.getFunction().apply(p, null), true);
	}

	/**
	 * Send a tab to a player
	 * 
	 * @param player
	 *            player to send the tab
	 * @param header
	 *            header text
	 * @param footer
	 *            footer text
	 * @since 1.1
	 */
	public static void sendTab(ProxiedPlayer player, String header, String footer) {
		SendTabEvent sendTabEvent = new SendTabEvent(header, footer, player);
		if (!ProxyServer.getInstance().getPluginManager().callEvent(sendTabEvent).isCancelled())
			sendTabEvent.getPlayer().setTabHeader(new TextComponent(sendTabEvent.getHeader()),
					new TextComponent(sendTabEvent.getFooter()));
	}

	private static File setFile(File file, String value) throws IOException {
		BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
		bw.write(value);
		bw.close();
		return file;
	}

	private CustomTabCommand command;

	List<OptionMatcher> getTextOptions() {
		return textOptions;
	}

	void loadConfig() throws IOException {
		File d = createDir(getDataFolder());
		File config = createFile(new File(d, "config.cfg"), "noBukkit: true");
		File header = createFile(new File(d, "header.cfg"), "%bukkitheadermessage%");
		File footer = createFile(new File(d, "footer.cfg"), "%bukkitfootermessage%");
		if (footer.exists() && header.exists() && config.exists()) {
			String[] c = loadFile(config).split("\n");
			for (String l : c) {
				while (l.startsWith(" "))
					l = l.substring(1);
				if (l.startsWith("#"))
					continue;
				String s = "noBukkit:";
				if (l.startsWith(s)) {
					l = l.substring(s.length());
					while (l.startsWith(" "))
						l = l.substring(1);
					if (l.startsWith("true")) {
						noBukkit = true;
					} else if (l.startsWith("false")) {
						noBukkit = false;
					}
				}
			}
			CustomTabPlugin.globalFooter = loadFile(footer);
			CustomTabPlugin.globalHeader = loadFile(header);
		} else
			throw new FileNotFoundException();
	}

	@Override
	public void onEnable() {
		try {
			loadConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}

		registerTextOption("name", p -> p.getName());
		registerTextOption("mainhand", p -> p.getMainHand().name().replace('_', ' '));
		registerTextOption("chatmode", p -> p.getChatMode().name().replace('_', ' '));
		registerTextOption("displayname", p -> p.getDisplayName());
		registerTextOption("servername", p -> p.getServer().getInfo().getName());
		registerTextOption("servermotd", p -> p.getServer().getInfo().getMotd());
		registerTextOption("playerscounts", p -> String.valueOf(this.getProxy().getOnlineCount()));
		registerTextOption("ping", p -> String.valueOf(p.getPing()));

		registerTextOption("cping", p -> {
			long ping = p.getPing();
			return (ping < 0 ? ChatColor.WHITE
					: (ping < 150 ? ChatColor.DARK_GREEN
							: (ping < 300 ? ChatColor.GREEN
									: (ping < 600 ? ChatColor.GOLD
											: (ping < 1000 ? ChatColor.RED : ChatColor.DARK_RED))))).toString()
					+ String.valueOf(ping);
		});

		registerTextOption(Pattern.compile("%date((-.+)?){1}%"), "%date%", (p, m) -> {
			String format = m.group(1);
			return new SimpleDateFormat(format.isEmpty() ? "HH:mm:ss" : format.substring(1))
					.format(Calendar.getInstance().getTime());
		}, (p, om) -> new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()), true);

		char[] ALL_COLORS = { 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
		char[] LIGHT_COLOR = { 'a', 'b', 'c', 'd', 'e', 'f' };
		char[] HEAVY_COLORS = { '2', '3', '4', '5', '6', '7' };
		char[] FORMAT = { 'o', 'l', 'm', 'n', 'r', 'k' };

		registerTextOption(Pattern.compile("%s(wap)?(((-[a-fA-F0-9k-oK-OrR]+)?)|l(ight)?|f(ormat)?|h(eavy)?)?%"),
				"%swap%|%swap-<colors>%|%swapheavy%|%swaplight%|%swapformat%", (p, m) -> {
					String format = m.group(2);
					char[] chars = format.isEmpty() ? ALL_COLORS
							: format.toLowerCase().matches("h(eavy)?") ? HEAVY_COLORS
									: format.toLowerCase().matches("l(ight)?") ? LIGHT_COLOR
											: format.toLowerCase().matches("f(ormat)?") ? FORMAT
													: format.substring(1).toCharArray();
					return new String(
							new char[] { ChatColor.COLOR_CHAR, chars[(int) (getSystemTimeInSecond() % chars.length)] });
				});

		// I use add(0, ...) to allow options swapping
		textOptions.add(0, new OptionMatcher(Pattern.compile("%s(wap)?t(ext)?(-[0-9]+)?-(.+)?"),
				"%swaptext(-delay)?-text1;;text2;;...%", (p, m) -> {
					String d = m.group(3);
					String[] formats = m.group(4).split(";;");
					return formats[(int) ((getSystemTimeInSecond()
							/ Math.max(1, d.isEmpty() ? 1L : Long.valueOf(d.substring(1)).longValue()))
							% formats.length)];
				}, null, false));

		getProxy().getScheduler().schedule(this,
				() -> getProxy().getPlayers().stream().filter(p -> p.getServer() != null).forEach(p -> {
					if (noBukkit)
						sendTab(p, getOptionnedText(globalHeader, p), getOptionnedText(globalFooter, p));
					else {
						ByteArrayDataOutput out = ByteStreams.newDataOutput();
						Map<String, Object> hm = new HashMap<String, Object>();
						hm.put("footer", globalFooter);
						hm.put("header", globalHeader);
						hm.put("player", p.getName());
						out.writeUTF(new GsonBuilder().create().toJson(hm));
						p.getServer().sendData(CHANNEL_NAME, out.toByteArray());
					}
				}), 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
		getProxy().getPluginManager().registerCommand(this, command = new CustomTabCommand(this));
		getProxy().getPluginManager().registerListener(this, this);
		getProxy().registerChannel(CHANNEL_NAME);
		super.onEnable();
	}

	@EventHandler
	public void onPluginMessage(PluginMessageEvent ev) {
		if (ev.getTag().equalsIgnoreCase(CHANNEL_NAME) && ev.getReceiver() != null
				&& ev.getReceiver() instanceof ProxiedPlayer) {
			ProxiedPlayer player = (ProxiedPlayer) ev.getReceiver();
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ev.getData()));
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> hm = new GsonBuilder().create().fromJson(dis.readUTF(), HashMap.class);
				sendTab(player,
						getOptionnedText(hm.containsKey("header") ? String.valueOf(hm.get("header")) : "", player),
						getOptionnedText(hm.containsKey("footer") ? String.valueOf(hm.get("footer")) : "", player));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@EventHandler
	public void onTabComplete(TabCompleteEvent ev) {
		if (ev.isCancelled())
			return;
		String cursor = ev.getCursor();
		if (ev.getCursor().startsWith("/") && ev.getSender() instanceof ProxiedPlayer)
			cursor = cursor.substring(1);

		String[] args = ev.getCursor().split(" ", 2);
		if (args.length == 2
				&& (args[0].equalsIgnoreCase(command.getName())
						|| Arrays.asList(command.getAliases()).contains(args[0]))
				&& ev.getSender() instanceof CommandSender)
			ev.getSuggestions()
					.addAll(asArrayList(command.onTabComplete((CommandSender) ev.getSender(), args[1].split(" "))));
		else if (command.getName().toLowerCase().startsWith(args[0].toLowerCase()))
			ev.getSuggestions().add(command.getName());
		else
			for (String alias : command.getAliases())
				if (alias.toLowerCase().startsWith(args[0].toLowerCase()))
					ev.getSuggestions().add(alias);
	}

	void saveConfig() throws IOException {
		File d = createDir(getDataFolder());
		setFile(new File(d, "footer.cfg"), globalFooter);
		setFile(new File(d, "header.cfg"), globalHeader);
	}

	void setGlobalFooter(String globalFooter) {
		CustomTabPlugin.globalFooter = globalFooter;
	}

	void setGlobalHeader(String globalHeader) {
		CustomTabPlugin.globalHeader = globalHeader;
	}

}
