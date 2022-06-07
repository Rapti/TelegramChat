package de.Linus122.Telegram;

import com.google.gson.*;
import de.Linus122.TelegramChat.TelegramChat;
import de.Linus122.TelegramComponents.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Telegram {
	public JsonObject authJson;
	public boolean connected = false;

	static int lastUpdate = 0;
	public String token;

	private List<TelegramActionListener> listeners = new ArrayList<TelegramActionListener>();

	private final String API_URL_GETME = "https://api.telegram.org/bot%s/getMe";
	private final String API_URL_GETUPDATES = "https://api.telegram.org/bot%s/getUpdates?offset=%d";
	private final String API_URL_GENERAL = "https://api.telegram.org/bot%s/%s";

	private Gson gson = new Gson();

	public void addListener(TelegramActionListener actionListener) {
		listeners.add(actionListener);
	}

	public boolean auth(String token) {
		this.token = token;
		return reconnect();
	}

	public boolean reconnect() {
		try {
			JsonObject obj = sendGet(String.format(API_URL_GETME, token));
			authJson = obj;
			System.out.print("[Telegram] Established a connection with the telegram servers.");
			connected = true;
			return true;
		} catch (Exception e) {
			connected = false;
			System.out.print("[Telegram] Sorry, but could not connect to Telegram servers. The token could be wrong.");
			return false;
		}
	}

	public boolean getUpdate() {
		JsonObject up = null;
		try {
			up = sendGet(String.format(API_URL_GETUPDATES, TelegramChat.getBackend().getToken(), lastUpdate + 1));
		} catch (IOException e) {
			return false;
		}
		if (up == null) {
			return false;
		}
		if (up.has("result")) {
			for (JsonElement ob : up.getAsJsonArray("result")) {
				if (ob.isJsonObject()) {
					Update update = gson.fromJson(ob, Update.class);
		
					if(lastUpdate == update.getUpdate_id()) return true;
					lastUpdate = update.getUpdate_id();

					if (update.getMessage() != null) {
						Chat chat = update.getMessage().getChat();

						if (chat.isPrivate()) {
							// private chat
							if (!TelegramChat.getBackend().chat_ids.contains(chat.getId()))
								TelegramChat.getBackend().chat_ids.add(chat.getId());

							String text = update.getMessage().getText();
							if (text != null && text.equals("/start")) {
								if (TelegramChat.getBackend().isFirstUse()) {
									TelegramChat.getBackend().setFirstUse(false);
									ChatMessageToTelegram chat2 = new ChatMessageToTelegram();
									chat2.chat_id = chat.getId();
									chat2.parse_mode = "Markdown";
									chat2.text = Utils.formatMSG("setup-msg")[0];
									this.sendMsg(chat2);
								}
								this.sendMsg(chat.getId(), Utils.formatMSG("can-see-but-not-chat")[0]);
							} else {
								handleUserMessage(text, update);
							}

						} else if (!chat.isPrivate()) {
							// group chat
							long id = chat.getId();
							if (!TelegramChat.getBackend().chat_ids.contains(id))
								TelegramChat.getBackend().chat_ids.add(id);

							String text = update.getMessage().getText();
							handleUserMessage(text, update);
						}
					}

				}
			}
		}
		return true;
	}
	
	public void handleUserMessage(String text, Update update) {
		if(text == null) text = "";
		final Message message = update.getMessage();
		MessageType mt = message.getType();
		Chat chat = message.getChat();
		long user_id = message.getFrom().getId();
		String tg_name = message.getFrom().getFirst_name();
		if (TelegramChat.getBackend().getLinkCodes().containsKey(text)) {
			// LINK
			TelegramChat.link(TelegramChat.getBackend().getUUIDFromLinkCode(text), user_id);
			TelegramChat.getBackend().removeLinkCode(text);
		} else {
			ChatMessageToMc chatMsg = null;
			if (TelegramChat.getBackend().getLinkedChats().containsKey(user_id)) {
				chatMsg = new ChatMessageToMc(
						TelegramChat.getBackend().getUUIDFromUserID(user_id), text, chat.getId(),tg_name);
			} else if(TelegramChat.getCfg().getBoolean("no-link-required")) {
				chatMsg = new ChatMessageToMc(text, chat.getId(),tg_name);
			}

			if(chatMsg != null) {
				if(mt != MessageType.TEXT && mt != MessageType.UNKNOWN) {
					chatMsg.setType(mt);
					chatMsg.setContent(message.getTextContent());
				}
				
				for (TelegramActionListener actionListener : listeners) {
					actionListener.onSendToMinecraft(chatMsg);
				}

				if(!chatMsg.isCancelled()){
					TelegramChat.sendToMC(chatMsg);
				}
			} else {
				this.sendMsg(chat.getId(), Utils.formatMSG("need-to-link")[0]);
			}
		}
	}

	public void sendMsg(long id, String msg) {
		ChatMessageToTelegram chat = new ChatMessageToTelegram();
		chat.chat_id = id;
		chat.text = msg;
		sendMsg(chat);
	}

	public void sendMsg(ChatMessageToTelegram chat) {
		for (TelegramActionListener actionListener : listeners) {
			actionListener.onSendToTelegram(chat);
		}
		Gson gson = new Gson();
		if(!chat.isCancelled()){
			post("sendMessage", gson.toJson(chat, ChatMessageToTelegram.class));	
		}
	}

	public void sendAll(final ChatMessageToTelegram chat) {
		new Thread(new Runnable() {
			public void run() {
				for (long id : TelegramChat.getBackend().chat_ids) {
					chat.chat_id = id;
					// post("sendMessage", gson.toJson(chat, Chat.class));
					sendMsg(chat);
				}
			}
		}).start();
	}

	public void updateGroupDescOffline() {updateGroupDesc(null, true);}
	public void updateGroupDesc() {updateGroupDesc(null, false);}
	public void updateGroupDesc(Event e) {updateGroupDesc(e, false);}
	public void updateGroupDesc(Event e, boolean goingOffline) {
		if(!TelegramChat.getCfg().getBoolean("change-group-description", false)) return;

		// Maximum description length defined by Telegram.
		// See https://core.telegram.org/bots/api#setchatdescription
		final int maxlength = 255;

		String base = TelegramChat.getCfg().getString("group-description.base", "%s");
		String status = "";
		if(goingOffline) {
			status = TelegramChat.getCfg().getString("group-description.offline", "The server is offline.");
		} else {
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			if (e instanceof PlayerQuitEvent) {
				LinkedList<Player> list = new LinkedList<>();
				for(Player p: players) {
					if (!p.equals(((PlayerQuitEvent) e).getPlayer())) {
						list.add(p);
					}
				}
				players = list;
			}
			if (players.size() == 0) {
				status = TelegramChat.getCfg().getString("group-description.nobody-online", "There are no players currently online.");
			} else {
				status = String.format(TelegramChat.getCfg().getString("group-description.num-players", "%s player(s) online"), players.size());
				String format = TelegramChat.getCfg().getString("group-description.player", "%s");
				String playerlist = "";
				int remaining = players.size();
				for (Player p : players) {
					final int length = String.format(base, status).length();
					String line = "\n"+String.format(format, p.getDisplayName());
					String fallback = "\n+"+remaining;
					if(length + line.length() <= maxlength) {
						status += line;
					} else {
						if(length + fallback.length() <= maxlength) {
							status += fallback;
						}
						break;
					}
				}
			}
		}

		String desc;
		desc = String.format(base, status);
		final ChatDescription update = new ChatDescription();
		update.description = desc;
		new Thread(() -> {
			for (long id : TelegramChat.getBackend().chat_ids) {
				if(id >= 0) continue; // Disregard private chats
				update.chat_id = id;
				post("setChatDescription", gson.toJson(update, ChatDescription.class));
			}
		}).start();
	}

	public JsonObject post(String method, String json) {
		Bukkit.getLogger().fine("Sending JSON " + json);
		try {
			String body = json;
			URL url = new URL(String.format(API_URL_GENERAL, TelegramChat.getBackend().getToken(), method));
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/json; ; Charset=UTF-8");
			connection.setRequestProperty("Content-Length", String.valueOf(body.length()));

			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
			writer.write(body);
			writer.close();
			wr.close();

			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder sb = new StringBuilder();

			try {
				String inputLine;
				while ((inputLine = br.readLine()) != null) {
					sb.append(inputLine);
				}
				br.close();

				return new JsonParser().parse(sb.toString()).getAsJsonObject();
			} catch (JsonParseException | IllegalStateException ignored) {
			} finally {
				writer.close();
				br.close();
			}
		} catch (Exception e) {
			reconnect();
			System.out.print("[Telegram] Disconnected from Telegram, reconnect...");
		}
		return null;
	}

	public JsonObject sendGet(String url) throws IOException {
		String a = url;
		URL url2 = new URL(a);
		URLConnection conn = url2.openConnection();

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

		String all = "";
		String inputLine;
		while ((inputLine = br.readLine()) != null) {
			all += inputLine;
		}

		br.close();
		JsonParser parser = new JsonParser();
		return parser.parse(all).getAsJsonObject();

	}

}
