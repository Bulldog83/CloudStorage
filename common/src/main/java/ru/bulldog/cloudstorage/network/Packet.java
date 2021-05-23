package ru.bulldog.cloudstorage.network;

import java.io.Serializable;

public abstract class Packet implements Serializable {

	private final PacketType type;

	protected Packet(PacketType type) {
		this.type = type;
	}

	public PacketType getType() {
		return type;
	}

	public enum PacketType {
		FILE,
		FILES_LIST,
		FILE_REQUEST,
		LIST_REQUEST
	}
}
