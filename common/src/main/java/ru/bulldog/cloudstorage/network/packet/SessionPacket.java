package ru.bulldog.cloudstorage.network.packet;

import java.util.UUID;

public class SessionPacket extends Packet {

	public SessionPacket(UUID uuid) {
		super(PacketType.SESSION, uuid);
	}
}
