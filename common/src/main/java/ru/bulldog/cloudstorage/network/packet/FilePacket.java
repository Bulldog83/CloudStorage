package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePacket extends Packet {

	private final String name;
	private final long size;

	public FilePacket(Path file) throws IOException {
		super(PacketType.FILE);
		this.name = file.getFileName().toString();
		this.size = Files.size(file);
	}

	protected FilePacket(ByteBuf buffer) {
		super(PacketType.FILE);
		int len = buffer.readInt();
		this.name = buffer.readCharSequence(len, StandardCharsets.UTF_8).toString();
		this.size = buffer.readLong();
	}

	public String getName() {
		return name;
	}

	public long getSize() {
		return size;
	}

	@Override
	public void write(ByteBuf buffer) throws Exception {

	}

	public void read(Path file, ByteBuf buffer) throws IOException {
		buffer.readBytes(Files.newOutputStream(file), (int) size);
	}
}
