package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;

public class ListRequest extends Packet {

	public ListRequest() {
		super(PacketType.LIST_REQUEST);
	}

	@Override
	public void write(ByteBuf buffer) throws Exception {
		buffer.writeByte(type.getIdx());
	}
}
