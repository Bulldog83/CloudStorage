package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;
import ru.bulldog.cloudstorage.data.DataBuffer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class FileRequest extends Packet {

	private final String name;

	public FileRequest(UUID session, String name) {
		super(PacketType.FILE_REQUEST, session);
		this.name = name;
	}

	public FileRequest(DataBuffer buffer) {
		super(PacketType.FILE_REQUEST, buffer);
		this.name = buffer.readString();
	}

	public String getName() {
		return name;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(name);
	}
}
