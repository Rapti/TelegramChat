package de.Linus122.Handlers;

import de.Linus122.Telegram.Telegram;
import de.Linus122.Telegram.TelegramActionListener;
import de.Linus122.TelegramChat.TelegramChat;
import de.Linus122.TelegramComponents.ChatMessageToMc;
import de.Linus122.TelegramComponents.ChatMessageToTelegram;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

public class PlayerListCommandHandler implements TelegramActionListener {

	private final Telegram telegram;

	public PlayerListCommandHandler(Telegram telegram) {
		this.telegram = telegram;
	}

	@Override
	public void onSendToTelegram(ChatMessageToTelegram chat) {

	}

	@Override
	public void onSendToMinecraft(ChatMessageToMc chatMsg) {
		if(chatMsg.getContent().startsWith("/players")) {

			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			StringBuilder reply = new StringBuilder();

			if (players.size() == 0) {
				reply = new StringBuilder(TelegramChat.getCfg().getString("group-description.nobody-online", "There are no players currently online."));
			} else {
				reply = new StringBuilder(String.format(TelegramChat.getCfg().getString("group-description.num-players", "%s player(s) online"), players.size()));
				String format = TelegramChat.getCfg().getString("group-description.player", "%s");
				for (Player p : players) {
					reply.append("\n").append(String.format(format, p.getDisplayName()));
				}
			}
			telegram.sendMsg(new ChatMessageToTelegram(reply.toString(), chatMsg.getChatID_sender(), "Markdown"));

			chatMsg.cancel();
		}
	}
}
