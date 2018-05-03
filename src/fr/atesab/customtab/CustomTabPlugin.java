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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.GsonBuilder;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
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
	 * Rate between every tab change
	 */
	public static final long REFRESH_RATE = 1000L;

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

	public static String getOptionnedText(String raw, ProxiedPlayer p) {
		for (String key : textOptions.keySet())
			raw = raw.replaceAll("%" + key + "%", textOptions.get(key).apply(p));
		return raw.replace('&', ChatColor.COLOR_CHAR);
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
	 * @param name name of the option to add
	 * @param option text option associated with the specified name
	 * @throws IllegalArgumentException if the name does contain a non-alphanumerics characters
	 */
	public static void registerTextOption(String name, Function<ProxiedPlayer, String> option) {
		if(!name.matches("[A-Za-z0-9\\_]*")) throw new IllegalArgumentException("name can only contain alphanumerics characters");
		textOptions.put(name, option);
	}
	/**
	 * Send a tab to a player
	 * @param player player to send the tab
	 * @param header header text
	 * @param footer footer text
	 */
	public static void sendTab(ProxiedPlayer player, String header, String footer) {
		player.setTabHeader(new TextComponent(header), new TextComponent(footer));
	}

	private void loadConfigs() throws IOException {
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
			loadConfigs();
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
		getProxy().getScheduler().schedule(this, () -> {
			for (ProxiedPlayer pp : getProxy().getPlayers()) {
				if (pp.getServer() == null)
					continue;
				if (noBukkit) {
					sendTab(pp, getOptionnedText(globalHeader, pp), getOptionnedText(globalFooter, pp));
				} else {
					if (pp.getServer() == null)
						continue;
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					Map<String, Object> hm = new HashMap<String, Object>();
					hm.put("footer", globalFooter);
					hm.put("header", globalHeader);
					hm.put("player", pp.getName());
					out.writeUTF(new GsonBuilder().create().toJson(hm));
					pp.getServer().sendData(CHANNEL_NAME, out.toByteArray());
				}
			}
		}, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
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
}
