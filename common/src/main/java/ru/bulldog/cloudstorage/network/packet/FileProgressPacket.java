package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

import java.util.UUID;

public class FileProgressPacket extends Packet {

	private final double progress;

	public FileProgressPacket(UUID session, double progress) {
		super(PacketType.FILE_PROGRESS, session);
		this.progress = progress;
	}

	protected FileProgressPacket(DataBuffer buffer) {
		super(PacketType.FILE_PROGRESS, buffer);
		this.progress = buffer.readDouble();
	}

	public double getProgress() {
		return progress;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeDouble(progress);
	}
}
