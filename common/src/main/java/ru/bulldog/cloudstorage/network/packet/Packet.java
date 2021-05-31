package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;
import ru.bulldog.cloudstorage.data.DataBuffer;

import java.io.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public abstract class Packet implements Serializable {

	protected final PacketType type;
	protected final UUID session;

	protected Packet(PacketType type, UUID session) {
		this.session = session;
		this.type = type;
	}

	public UUID getSession() {
		return session;
	}

	public PacketType getType() {
		return type;
	}

	public void write(DataBuffer buffer) throws Exception {
		buffer.writeByte(type.getIdx());
		buffer.writeUUID(session);
	}

	public static Optional<Packet> read(DataBuffer buffer) {
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
				case FILE_PROGRESS:
					return Optional.of(new FileProgressPacket(buffer));
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
		FILE_PROGRESS(15),
		UUID(16),
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
