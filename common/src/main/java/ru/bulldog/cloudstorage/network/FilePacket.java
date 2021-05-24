package ru.bulldog.cloudstorage.network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePacket extends Packet {

	private final String name;
	private final long size;
	private final byte[] data;

	public FilePacket(Path file) throws IOException {
		super(PacketType.FILE);
		this.name = file.getFileName().toString();
		this.size = Files.size(file);
		this.data = Files.readAllBytes(file);
	}

	public String getName() {
		return name;
	}

	public long getSize() {
		return size;
	}

	public byte[] getData() {
		return data;
	}

	public boolean isEmpty() {
		return data.length == 0;
	}
}
