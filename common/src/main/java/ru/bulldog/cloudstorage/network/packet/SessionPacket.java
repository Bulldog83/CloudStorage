package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

import java.util.UUID;

public class SessionPacket extends Packet {

	private final UUID session;

	public SessionPacket(UUID uuid) {
		super(PacketType.SESSION);
		this.session = uuid;
	}

	protected SessionPacket(DataBuffer buffer) {
		super(PacketType.SESSION);
		this.session = buffer.readUUID();
	}

	public UUID getSession() {
		return session;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeUUID(session);
	}
}
