package ru.bulldog.cloudstorage.network;

import ru.bulldog.cloudstorage.network.packet.Packet;

public interface NetworkHandler extends AutoCloseable {
	void handlePacket(Session session, Packet packet);
}
