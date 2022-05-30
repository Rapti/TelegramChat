package de.Linus122.TelegramComponents;

public class Cancellable {
	private boolean isCancelled = false;

	public boolean isCancelled() {
		return isCancelled;
	}

	public void cancel() {
		this.isCancelled = true;
	}
}
