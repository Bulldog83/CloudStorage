package ru.bulldog.cloudstorage.network;

import java.io.*;

public abstract class Packet implements Serializable {

	private final PacketType type;

	protected Packet(PacketType type) {
		this.type = type;
	}

	public PacketType getType() {
		return type;
	}

	public enum PacketType {
		FILE,
		FILES_LIST,
		FILE_REQUEST,
		LIST_REQUEST
	}

	public byte[] toByteArray() throws IOException {
		try(ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ObjectOutputStream objOut = new ObjectOutputStream(byteOut)) {

			objOut.writeObject(this);
			return byteOut.toByteArray();
		}
	}

	public static Packet fromByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
		try(ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
			ObjectInputStream objIn = new ObjectInputStream(byteIn)) {

			return (Packet) objIn.readObject();
		}
	}
}
