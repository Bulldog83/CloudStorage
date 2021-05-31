package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;
import ru.bulldog.cloudstorage.data.DataBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FilePacket extends Packet {

	private final String name;
	private final long size;
	private File file;

	public FilePacket(UUID sessionId, Path file) throws IOException {
		super(PacketType.FILE, sessionId);
		this.name = file.getFileName().toString();
		this.size = Files.size(file);
		this.file = file.toFile();
	}

	protected FilePacket(DataBuffer buffer) {
		super(PacketType.FILE, buffer);
		this.name = buffer.readString();
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
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(name)
				.writeLong(size);
	}

	public boolean isEmpty() {
		return file == null || !file.exists();
	}
}
