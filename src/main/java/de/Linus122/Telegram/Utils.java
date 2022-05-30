package de.Linus122.Telegram;

import de.Linus122.TelegramChat.TelegramChat;

public class Utils {
	public static String escape(String str) {
		return str.replace("_", "\\_");
	}

	final static String MESSAGE_SECTION = "messages";

	public static String[] formatMSG(String suffixKey) {
		return formatMSG(suffixKey, "");
	}

	public static String[] formatMSG(String suffixKey, Object... args) {
		String key = MESSAGE_SECTION + "." + suffixKey;
		if (!TelegramChat.getCfg().contains(key))
			return new String[] {
					"Message not found in config.yml. Please check your config if the following key is present:", key };
		String rawMessage = TelegramChat.getCfg().getString(key);
		if (args != null && args.length > 0)
			rawMessage = String.format(rawMessage, args);
		rawMessage = rawMessage.replace("&", "§");

		return rawMessage.split("\\n");

	}
}
