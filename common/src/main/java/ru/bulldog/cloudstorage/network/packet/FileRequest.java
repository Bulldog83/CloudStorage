package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

import java.util.UUID;

public class FileRequest extends Packet {

	private final UUID session;
	private final String name;

	public FileRequest(UUID session, String name) {
		super(PacketType.FILE_REQUEST);
		this.session = session;
		this.name = name;
	}

	public FileRequest(DataBuffer buffer) {
		super(PacketType.FILE_REQUEST);
		this.session = buffer.readUUID();
		this.name = buffer.readString();
	}

	public UUID getSession() {
		return session;
	}

	public String getName() {
		return name;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeUUID(session)
			  .writeString(name);
	}
}
