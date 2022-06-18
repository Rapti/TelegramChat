package de.Linus122.TelegramComponents;

public class ChatDescription {
	public long chat_id;
	public String description;

	// Chat Descriptions only support plain text right now ...
	// I'll leave it here in case support gets added in the future.
	// Go ahead and nag the Telegram devs if you want this feature to work!
	// https://core.telegram.org/bots/api#setchatdescription

	//public String parse_mode = "Markdown";
}
