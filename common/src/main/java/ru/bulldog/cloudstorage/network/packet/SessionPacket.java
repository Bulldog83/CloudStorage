package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

import java.util.UUID;

public class SessionPacket extends Packet {

	public SessionPacket(UUID uuid) {
		super(PacketType.SESSION, uuid);
	}

	public SessionPacket(DataBuffer buffer) {
		super(PacketType.SESSION, buffer);
	}
}
