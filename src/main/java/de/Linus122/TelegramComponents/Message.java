package de.Linus122.TelegramComponents;

public class Message {
	private int message_id;
	private User from;
	private int date;
	private Chat chat;
	private String text;

	private GeneralMedia[] photo;
	private GeneralMedia audio;
	private GeneralMedia document;
	private Object sticker;
	private GeneralMedia video;
	private GeneralMedia videoNote;
	private GeneralMedia voice;
	private String caption;
	private Poll poll;
	private Object location;
	private Message pinned_message;

	public int getMessage_id() {
		return message_id;
	}

	public void setMessage_id(int message_id) {
		this.message_id = message_id;
	}

	public User getFrom() {
		return from;
	}

	public void setFrom(User from) {
		this.from = from;
	}

	public int getDate() {
		return date;
	}

	public void setDate(int date) {
		this.date = date;
	}

	public Chat getChat() {
		return chat;
	}

	public void setChat(Chat chat) {
		this.chat = chat;
	}

	public String getText() {
		return text;
	}

	public String getTextContent() {
		switch (getType()) {
			case TEXT: return text;
			case POLL: return poll.question;
			case PINNED: return pinned_message.getTextContent();
			default: return caption;
		}
	}

	public void setText(String text) {
		this.text = text;
	}

	public MessageType getType() {
		if(photo != null && photo.length > 0) return MessageType.PHOTO;
		if(audio != null) return MessageType.AUDIO;
		if(video != null) return MessageType.VIDEO;
		if(voice != null) return MessageType.VOICE;
		if(videoNote != null) return MessageType.VIDEONOTE;
		if(poll != null) return MessageType.POLL;
		if(location != null) return MessageType.LOCATION;
		if(pinned_message != null) return MessageType.PINNED;
		if(sticker != null) return MessageType.STICKER;
		if(document != null) return MessageType.DOCUMENT;
		if(text != null) return MessageType.TEXT;
		return MessageType.UNKNOWN;
	}
}
