package ru.bulldog.cloudstorage.event;

import ru.bulldog.cloudstorage.network.packet.FilesListPacket;

public interface ActionListener {
	default void onFilesList(FilesListPacket filesList) {}
	default void onFileStart(String direction, String fileName) {}
	default void onFileProgress(double progress) {}
	default void onFileReceived() {}
	default void onMessageReceived(String message) {}
	default void onHandleError(String message) {}
	default void onDisconnect() {}
	default void onConnect() {}
}
