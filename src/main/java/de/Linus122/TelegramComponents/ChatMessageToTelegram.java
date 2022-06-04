package de.Linus122.TelegramComponents;

public class ChatMessageToTelegram extends Cancellable {
	public String text;

	public ChatMessageToTelegram() {
	}

	public ChatMessageToTelegram(String text, long chat_id, String parse_mode) {
		this.text = text;
		this.chat_id = chat_id;
		this.parse_mode = parse_mode;
	}

	public long chat_id;
	public String parse_mode;
}
