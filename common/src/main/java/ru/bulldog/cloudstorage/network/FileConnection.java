package ru.bulldog.cloudstorage.network;

import io.netty.channel.socket.SocketChannel;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.util.Optional;
import java.util.UUID;

public class FileConnection extends Connection {

	private final ReceivingFile receivingFile;
	private final SocketChannel channel;

	public FileConnection(Connection connection, SocketChannel channel, ReceivingFile receivingFile) {
		super(connection.getChannel(), connection.getUUID());
		this.channel = channel;
		this.receivingFile = receivingFile;
	}

	public ReceivingFile getReceivingFile() {
		return receivingFile;
	}

	@Override
	public boolean isFileConnection() {
		return true;
	}

	@Override
	public void close() throws Exception {
		channel.close();
	}
}
