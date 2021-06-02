package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

import java.util.UUID;

public class ListRequest extends Packet {
	public ListRequest(UUID session) {
		super(PacketType.LIST_REQUEST, session);
	}

	public ListRequest(DataBuffer buffer) {
		super(PacketType.LIST_REQUEST, buffer);
	}
}
