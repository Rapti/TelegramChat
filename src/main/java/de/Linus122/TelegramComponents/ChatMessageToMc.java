package de.Linus122.TelegramComponents;

import java.util.UUID;

public class ChatMessageToMc extends Cancellable {
	UUID uuid_sender;
	String tgname_sender;
	String content;
	long chatID_sender;

	public ChatMessageToMc(String content, long chatID_sender, String tgname_sender) {
		this(null, content, chatID_sender, tgname_sender);
	}
	public ChatMessageToMc(UUID uuid_sender, String content, long chatID_sender, String tgname_sender) {
		this(uuid_sender, content, chatID_sender);
		this.tgname_sender = tgname_sender;
	}
	public ChatMessageToMc(UUID uuid_sender, String content, long chatID_sender) {
		this.uuid_sender = uuid_sender;
		this.content = content;
		this.chatID_sender = chatID_sender;
	}

	public UUID getUuid_sender() {
		return uuid_sender;
	}

	public void setUuid_sender(UUID uuid_sender) {
		this.uuid_sender = uuid_sender;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public long getChatID_sender() {
		return chatID_sender;
	}

	public void setChatID_sender(long chatID_sender) {
		this.chatID_sender = chatID_sender;
	}

	public String getTelegramName() {
		return tgname_sender;
	}

	public boolean senderIsLinked() {
		return uuid_sender != null;
	}
}
