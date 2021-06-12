package ru.bulldog.cloudstorage.event;

import ru.bulldog.cloudstorage.network.packet.FilesListPacket;

public interface FilesListener extends ActionListener {
	void onFileReceived();
	void onFilesList(FilesListPacket filesList);
}
