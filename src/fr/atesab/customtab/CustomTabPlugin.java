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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.GsonBuilder;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
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
	 */
	public static final String CHANNEL_NAME = "CustomTab";
	private static Map<String, Function<ProxiedPlayer, String>> textOptions = new HashMap<>();
	/**
	 * Rate between every tab change in millisecond
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
	 */
	public static String getGlobalFooter() {
		return globalFooter;
	}

	/**
	 * get the global raw text header
	 * 
	 * @return raw header
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
	 */
	public static String getOptionnedText(String raw, ProxiedPlayer player) {
		for (String key : textOptions.keySet())
			if(raw.contains("%" + key + "%"))
				raw = raw.replaceAll("%" + key + "%", textOptions.get(key).apply(player));
		return raw.replace('&', ChatColor.COLOR_CHAR);
	}

	/**
	 * @return if CustomTab ask to a Bukkit server or not local information
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
	 * @param name
	 *            name of the option to add
	 * @param option
	 *            text option associated with the specified name
	 * @throws IllegalArgumentException
	 *             if the name does contain a non-alphanumerics characters
	 */
	public static void registerTextOption(String name, Function<ProxiedPlayer, String> option) {
		if (!name.matches("[A-Za-z0-9\\_]*"))
			throw new IllegalArgumentException("name can only contain alphanumerics characters");
		textOptions.put(name, option);
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
	 */
	public static void sendTab(ProxiedPlayer player, String header, String footer) {
		player.setTabHeader(new TextComponent(header), new TextComponent(footer));
	}

	private static File setFile(File file, String value) throws IOException {
		BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
		bw.write(value);
		bw.close();
		return file;
	}

	private CustomTabCommand command;

	Map<String, Function<ProxiedPlayer, String>> getTextOptions() {
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
		registerTextOption("date", p -> new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
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
