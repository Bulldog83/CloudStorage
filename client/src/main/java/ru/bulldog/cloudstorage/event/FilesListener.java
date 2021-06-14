package ru.bulldog.cloudstorage.event;

import ru.bulldog.cloudstorage.network.packet.FilesListPacket;

public interface FilesListener extends ActionListener {
	void onFilesList(FilesListPacket filesList);
	void onFileStart(String direction, String fileName);
	void onFileProgress(double progress);
	void onFileReceived();
}
