package ru.bulldog.cloudstorage.event;

public interface ActionListener {
	void onMessageReceived(String message);
	void onDisconnect();
	void onConnect();

	default boolean isHandleFiles() {
		return this instanceof FilesListener;
	}
}
