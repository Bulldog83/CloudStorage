package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

public class ListRequest extends Packet {

	private final String path;
	private final boolean parent;

	public ListRequest(String path, boolean parent) {
		super(PacketType.LIST_REQUEST);
		this.path = path;
		this.parent = parent;
	}

	public ListRequest(DataBuffer buffer) {
		super(PacketType.LIST_REQUEST);
		this.path = buffer.readString();
		this.parent = buffer.readBoolean();
	}

	public String getPath() {
		return path;
	}

	public boolean isParent() {
		return parent;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(path);
		buffer.writeBoolean(parent);
	}
}
