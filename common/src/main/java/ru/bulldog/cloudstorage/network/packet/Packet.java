package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;

import java.io.*;
import java.util.Arrays;
import java.util.Optional;

public abstract class Packet implements Serializable {

	protected final PacketType type;

	protected Packet(PacketType type) {
		this.type = type;
	}

	public PacketType getType() {
		return type;
	}

	public abstract void write(ByteBuf buffer) throws Exception;

	public static Optional<Packet> read(ByteBuf buffer) {
		PacketType packetType = getType(buffer.readByte());
		if (packetType != PacketType.UNKNOWN) {
			switch (packetType) {
				case FILES_LIST:
					return Optional.of(new FilesListPacket(buffer));
				case LIST_REQUEST:
					return Optional.of(new ListRequest());
				case FILE:
					return Optional.of(new FilePacket(buffer));
				case FILE_REQUEST:
					return Optional.of(new FileRequest(buffer));
			}
		}
		return Optional.empty();
	}

	public static PacketType getType(byte idx) {
		return Arrays.stream(PacketType.values())
				.filter(type -> type.idx == idx).findFirst()
				.orElse(PacketType.UNKNOWN);
	}

	public enum PacketType {
		FILE(10),
		FILES_LIST(11),
		FILE_REQUEST(12),
		LIST_REQUEST(13),
		COMMAND_PACKET(14),
		UNKNOWN(-1);

		private final byte idx;

		PacketType(int idx) {
			this.idx = (byte) idx;
		}

		public byte getIdx() {
			return idx;
		}
	}
}
