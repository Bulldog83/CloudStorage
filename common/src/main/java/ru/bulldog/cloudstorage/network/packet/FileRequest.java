package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;

public class FileRequest extends Packet {

	private final String name;

	public FileRequest(String name) {
		super(PacketType.FILE_REQUEST);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public void write(ByteBuf buffer) throws Exception {

	}
}
