package ru.bulldog.cloudstorage.network;

import io.netty.channel.socket.SocketChannel;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.util.Optional;

public class FileChannel implements AutoCloseable {

	private final SocketChannel channel;
	private ReceivingFile receivingFile;

	public FileChannel(SocketChannel channel, ReceivingFile receivingFile) {
		this.channel = channel;
	}

	public Optional<ReceivingFile> getReceivingFile() {
		return Optional.ofNullable(receivingFile);
	}

	public boolean isReceiving() {
		return receivingFile != null;
	}

	@Override
	public void close() throws Exception {
		channel.close();
	}
}
