package ru.bulldog.cloudstorage.network.packet;

import io.netty.buffer.ByteBuf;
import ru.bulldog.cloudstorage.data.DataBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FilePacket extends Packet {

	private final UUID session;
	private final String name;
	private final long size;
	private ByteBuf buffer;
	private File file;

	public FilePacket(UUID sessionId, Path file) throws IOException {
		super(PacketType.FILE);
		this.session = sessionId;
		this.name = file.getFileName().toString();
		this.size = Files.size(file);
		this.file = file.toFile();
	}

	protected FilePacket(DataBuffer buffer) {
		super(PacketType.FILE);
		this.session = buffer.readUUID();
		this.name = buffer.readString();
		this.size = buffer.readLong();
		this.buffer = buffer;
	}

	public UUID getSession() {
		return session;
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

	public ByteBuf getBuffer() {
		return buffer;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeUUID(session)
			  .writeString(name)
			  .writeLong(size);
	}

	public boolean isEmpty() {
		return file == null || !file.exists();
	}
}
