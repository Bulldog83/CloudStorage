package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

public class ListRequest extends Packet {

	private final String path;

	public ListRequest(String path) {
		super(PacketType.LIST_REQUEST);
		this.path = path;
	}

	public ListRequest(DataBuffer buffer) {
		super(PacketType.LIST_REQUEST);
		this.path = buffer.readString();
	}

	public String getPath() {
		return path;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(path);
	}
}
