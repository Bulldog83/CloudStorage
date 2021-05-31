package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;

public class FileProgressPacket extends Packet {

	private final double progress;

	public FileProgressPacket(double progress) {
		super(PacketType.FILE_PROGRESS);
		this.progress = progress;
	}

	protected FileProgressPacket(ByteBuf buffer) {
		super(PacketType.FILE_PROGRESS);
		this.progress = buffer.readDouble();
	}

	public double getProgress() {
		return progress;
	}

	@Override
	public void write(ByteBuf buffer) throws Exception {
		super.write(buffer);
		buffer.writeDouble(progress);
	}
}
