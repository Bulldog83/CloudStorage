package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class FileRequest extends Packet {

	private final String name;

	public FileRequest(String name) {
		super(PacketType.FILE_REQUEST);
		this.name = name;
	}

	public FileRequest(ByteBuf buffer) {
		super(PacketType.FILE_REQUEST);
		int len = buffer.readInt();
		this.name = buffer.readCharSequence(len , StandardCharsets.UTF_8).toString();
	}

	public String getName() {
		return name;
	}

	@Override
	public void write(ByteBuf buffer) throws Exception {
		super.write(buffer);
		buffer.writeInt(name.length());
		buffer.writeBytes(name.getBytes(StandardCharsets.UTF_8));
	}
}
