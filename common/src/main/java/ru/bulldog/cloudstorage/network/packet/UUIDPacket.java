package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class UUIDPacket extends Packet {

	private final UUID uuid;

	public UUIDPacket(UUID uuid) {
		super(PacketType.UUID);
		this.uuid = uuid;
	}

	protected UUIDPacket(ByteBuf buffer) {
		super(PacketType.UUID);
		long mostBits = buffer.readLong();
		long leastBits = buffer.readLong();
		this.uuid = new UUID(mostBits, leastBits);
	}

	public UUID getUUID() {
		return uuid;
	}

	@Override
	public void write(ByteBuf buffer) throws Exception {
		super.write(buffer);
		buffer.writeLong(uuid.getMostSignificantBits());
		buffer.writeLong(uuid.getLeastSignificantBits());
	}
}
