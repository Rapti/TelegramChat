package de.Linus122.TelegramChat;

import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RecentQuitMessages {
	private final LinkedList<RecentQuitMessage> messages = new LinkedList<>();
	private final Lock lock = new ReentrantLock();
	private final int interval;

	public RecentQuitMessages(int interval) {
		this.interval = interval;
	}

	public void registerNewQuitMessage(Player p, long chat_id, int message_id) {
		if(interval <= 0) return;
		lock.lock();
		messages.add(new RecentQuitMessage(p.getUniqueId(), chat_id, message_id, Instant.now()));
		lock.unlock();
	}

	public boolean deleteOldQuitMessages(Player p) {
		if(interval <= 0) return false;
		lock.lock();
		Instant deadline = Instant.now().minusSeconds(interval * 60L);
		messages.removeIf((RecentQuitMessage rqm) -> rqm.time.isBefore(deadline));
		boolean result =  messages.removeIf((RecentQuitMessage rqm) -> {
			if(rqm.player.equals(p.getUniqueId())) {
				deleteMessage(rqm);
				return true;
			}
			return false;
		});
		lock.unlock();
		return result;
	}

	private void deleteMessage(RecentQuitMessage rqm) {
		TelegramChat.telegramHook.deleteMessage(rqm.chat_id, rqm.message_id);
	}
}

class RecentQuitMessage {
	final UUID player;
	final long chat_id;
	final int message_id;
	final Instant time;

	public RecentQuitMessage(UUID player, long chat_id, int message_id, Instant time) {
		this.player = player;
		this.chat_id = chat_id;
		this.message_id = message_id;
		this.time = time;
	}
}