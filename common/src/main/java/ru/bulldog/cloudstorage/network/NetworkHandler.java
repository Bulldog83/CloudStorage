package ru.bulldog.cloudstorage.network;

public interface NetworkHandler extends AutoCloseable {
	void handlePacket(Connection connection, Packet packet);
}
