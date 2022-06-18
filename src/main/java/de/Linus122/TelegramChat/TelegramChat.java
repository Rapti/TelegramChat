package de.Linus122.TelegramChat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import de.Linus122.Handlers.PlayerListCommandHandler;
import de.Linus122.Handlers.VanishHandler;
import de.Linus122.TelegramComponents.Message;
import de.myzelyam.api.vanish.VanishAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;

import de.Linus122.Handlers.BanHandler;
import de.Linus122.Metrics.Metrics;
import de.Linus122.Telegram.Telegram;
import de.Linus122.Telegram.Utils;
import de.Linus122.TelegramComponents.Chat;
import de.Linus122.TelegramComponents.ChatMessageToMc;
import de.Linus122.TelegramComponents.ChatMessageToTelegram;

import static de.Linus122.TelegramComponents.MessageType.TEXT;

public class TelegramChat extends JavaPlugin implements Listener {
	private static File datad = new File("plugins/TelegramChat/data.json");
	private static FileConfiguration cfg;

	private static Data data = new Data();
	public static Telegram telegramHook;
	private static TelegramChat instance;
	private static boolean isSuperVanish;

	private static RecentQuitMessages recentQuitMessageHandler;

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		cfg = this.getConfig();
		instance = this;

		recentQuitMessageHandler = new RecentQuitMessages(cfg.getInt("delete-quitmessages-within", 1));

		Bukkit.getPluginCommand("telegram").setExecutor(new TelegramCmd());
		Bukkit.getPluginCommand("linktelegram").setExecutor(new LinkTelegramCmd());
		Bukkit.getPluginManager().registerEvents(this, this);

		if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish") || Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
			isSuperVanish = true;
			Bukkit.getPluginManager().registerEvents(new VanishHandler(), this);
		}

		File dir = new File("plugins/TelegramChat/");
		dir.mkdir();
		data = new Data();
		if (datad.exists()) {
			Gson gson = new Gson();
			try {
				FileReader fileReader = new FileReader(datad);
				StringBuilder sb = new StringBuilder();
				int c;
			    while((c = fileReader.read()) !=-1) {
			    	sb.append((char) c);
			    }

				data = (Data) gson.fromJson(sb.toString(), Data.class);
				
				fileReader.close();
			} catch (Exception e) {
				// old method for loading the data.yml file
				try {
					FileInputStream fin = new FileInputStream(datad);
					ObjectInputStream ois = new ObjectInputStream(fin);
					
					data = (Data) gson.fromJson((String) ois.readObject(), Data.class);
					ois.close();
					fin.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				this.getLogger().log(Level.INFO, "Converted old data.yml");
				save();
			}
		}

		telegramHook = new Telegram();
		telegramHook.auth(data.getToken());
		
		// Ban Handler (Prevents banned players from chatting)
		telegramHook.addListener(new BanHandler());

		// Allows server commands to be issued from Telegram
		// telegramHook.addListener(new CommandHandler(telegramHook, this));

		// Allows Telegram users to see who's online
		telegramHook.addListener(new PlayerListCommandHandler(telegramHook));

		// Console sender handler, allows players to send console commands (telegram.console permission)
		// telegramHook.addListener(new CommandHandler(telegramHook, this));

		Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			boolean connectionLost = false;
			if (connectionLost) {
				boolean success = telegramHook.reconnect();
				if (success)
					connectionLost = false;
			}
			if (telegramHook.connected) {
				connectionLost = !telegramHook.getUpdate();
			}
		}, 10L, 10L);

		telegramHook.updateGroupDesc();

		new Metrics(this);
	}

	@Override
	public void onDisable() {
		save();
		telegramHook.updateGroupDescOffline();
	}

	public static void save() {
		Gson gson = new Gson();

		try {
			FileWriter fileWriter = new FileWriter(datad);
			fileWriter.write(gson.toJson(data));
			
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Data getBackend() {
		return data;
	}

	public static void initBackend() {
		data = new Data();
	}

	public static void sendToMC(ChatMessageToMc chatMsg) {
		String senderName;
		if(chatMsg.senderIsLinked())
			senderName = Bukkit.getOfflinePlayer(chatMsg.getUuid_sender()).getName();
		else 
			senderName = chatMsg.getSenderTelegramName();

		String msg = chatMsg.getContent();
		String msgF;
		if(chatMsg.getType() == TEXT) {
			msgF = Utils.formatMSG("general-message-to-mc", senderName, msg)[0];
		} else {
			String specialMessage = Utils.formatMSG(chatMsg.getType().toString().toLowerCase(), "")[0];
			if(specialMessage.isEmpty()) return; // Server admin doesn't want this message type to be forwarded to Minecraft
			if(msg != null && msg.length() > 0) {
				msgF = Utils.formatMSG("special-message-with-caption", senderName, specialMessage, msg)[0];
			} else {
				msgF = Utils.formatMSG("special-message-without-caption", senderName, specialMessage)[0];
			}
		}

		List<Long> receivers = new ArrayList<Long>();
		receivers.addAll(TelegramChat.data.chat_ids);
		receivers.remove((Object) chatMsg.getChatID_sender());
		for (long id : receivers) {
			telegramHook.sendMsg(id, msgF.replaceAll("§.", ""));
		}
		Bukkit.broadcastMessage(msgF.replace("&", "§"));
	}

	public static void link(UUID player, long userID) {
		TelegramChat.data.addChatPlayerLink(userID, player);
		OfflinePlayer p = Bukkit.getOfflinePlayer(player);
		telegramHook.sendMsg(userID, "Success! Linked " + p.getName());
	}
	
	public boolean isChatLinked(Chat chat) {
		if(TelegramChat.getBackend().getLinkedChats().containsKey(chat.getId())) {
			return true;
		}
		
		return false;
	}

	public static String generateLinkToken() {

		Random rnd = new Random();
		int i = rnd.nextInt(9999999);
		String s = i + "";
		String finals = "";
		for (char m : s.toCharArray()) {
			int m2 = Integer.parseInt(m + "");
			int rndi = rnd.nextInt(2);
			if (rndi == 0) {
				m2 += 97;
				char c = (char) m2;
				finals = finals + c;
			} else {
				finals = finals + m;
			}
		}
		return finals;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		telegramHook.updateGroupDesc(e);
		if (!this.getConfig().getBoolean("enable-joinquitmessages"))
			return;

		final Player player = e.getPlayer();

		if(isSuperVanish && VanishAPI.isInvisible(player))
			return;

		if (telegramHook.connected && !recentQuitMessageHandler.deleteOldQuitMessages(player)) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			chat.parse_mode = "Markdown";
			chat.text = Utils.formatMSG("join-message", player.getName())[0];
			chat.disable_notification = this.getConfig().getBoolean("silent-joinquitmessages", false);
			telegramHook.sendAll(chat, (Message m) -> Bukkit.getLogger().info("getChat returns null: " + (m.getChat() == null)));
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (!this.getConfig().getBoolean("enable-deathmessages"))
			return;
		if (telegramHook.connected) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			chat.parse_mode = "Markdown";
			chat.text = Utils.formatMSG("death-message", e.getDeathMessage())[0];
			chat.disable_notification = this.getConfig().getBoolean("silent-deathmessages", false);
			telegramHook.sendAll(chat);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		telegramHook.updateGroupDesc(e);
		if (!this.getConfig().getBoolean("enable-joinquitmessages"))
			return;

		final Player player = e.getPlayer();

		if(isSuperVanish && VanishAPI.isInvisible(player))
			return;

		if (telegramHook.connected) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			chat.parse_mode = "Markdown";
			chat.text = Utils.formatMSG("quit-message", player.getName())[0];
			chat.disable_notification = this.getConfig().getBoolean("silent-joinquitmessages", false);
			telegramHook.sendAll(chat, (Message m) -> recentQuitMessageHandler.registerNewQuitMessage(player, m.getChat().getId(), m.getMessage_id()));
		}
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if (!this.getConfig().getBoolean("enable-chatmessages"))
			return;
		if (e.isCancelled())
			return;
		if (telegramHook.connected) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			chat.parse_mode = "Markdown";
			chat.text = Utils
					.escape(Utils.formatMSG("general-message-to-telegram", e.getPlayer().getName(), e.getMessage())[0])
					.replaceAll("§.", "");
			chat.disable_notification = this.getConfig().getBoolean("silent-chatmessages", false);
			telegramHook.sendAll(chat);
		}
	}

	public static TelegramChat getInstance()
	{
		return instance;
	}


	public static FileConfiguration getCfg() {
		return cfg;
	}
}
