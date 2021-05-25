package ru.bulldog.cloudstorage.network.packet;

public class FileRequest extends Packet {

	private final String name;

	public FileRequest(String name) {
		super(PacketType.FILE_REQUEST);
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
