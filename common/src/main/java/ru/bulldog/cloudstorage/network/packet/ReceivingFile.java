package ru.bulldog.cloudstorage.network.packet;

import java.io.File;

public class ReceivingFile {
	private final File file;
	private final long size;
	private long received;

	public ReceivingFile(File file, long size) {
		this.file = file;
		this.size = size;
	}

	public File getFile() {
		return file;
	}

	public void receive(long size) {
		received += size;
	}

	public long getSize() {
		return size;
	}

	public long getReceived() {
		return received;
	}

	public long toReceive() {
		return size - received;
	}

	@Override
	public String toString() {
		return file.toString();
	}
}
