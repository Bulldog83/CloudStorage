package ru.bulldog.cloudstorage.network;

import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.IOException;

public interface Connection extends AutoCloseable {
	void sendData(Packet packet) throws IOException;
	boolean isConnected();
}
