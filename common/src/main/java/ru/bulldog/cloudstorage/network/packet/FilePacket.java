package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePacket extends Packet {

	private final String name;
	private final long size;
	private File file;

	public FilePacket(Path file) throws IOException {
		super(PacketType.FILE);
		this.name = file.getFileName().toString();
		this.size = Files.size(file);
		this.file = file.toFile();
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

	public File getFile() {
		return file;
	}

	@Override
	public void write(ByteBuf buffer) throws Exception {
		buffer.writeByte(type.getIdx());
		buffer.writeInt(name.length());
		buffer.writeBytes(name.getBytes(StandardCharsets.UTF_8));
		buffer.writeLong(size);
	}

	public boolean isEmpty() {
		return file == null || !file.exists();
	}
}
