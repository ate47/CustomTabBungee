package fr.atesab.customtab;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

/**
 * An event call every time CustomTab send a new tab to a player
 * 
 * @author ATE47
 * @since 1.3
 */
public class SendTabEvent extends Event implements Cancellable {
	private boolean cancelled = false;
	private String header;
	private String footer;
	private ProxiedPlayer player;

	public SendTabEvent(String header, String footer, ProxiedPlayer player) {
		this.header = header;
		this.footer = footer;
		this.player = player;
	}

	public String getFooter() {
		return footer;
	}

	public String getHeader() {
		return header;
	}

	public ProxiedPlayer getPlayer() {
		return player;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}

	public void setHeader(String header) {
		this.header = header;
	}

}
