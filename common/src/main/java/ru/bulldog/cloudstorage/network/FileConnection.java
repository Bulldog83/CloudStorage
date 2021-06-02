package ru.bulldog.cloudstorage.network;

import io.netty.channel.socket.SocketChannel;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

public class FileConnection extends Connection {

	private final ReceivingFile receivingFile;
	private final SocketChannel fileChannel;

	public FileConnection(Connection connection, SocketChannel channel, ReceivingFile receivingFile) {
		super(connection.getChannel(), connection.getUUID());
		this.fileChannel = channel;
		this.receivingFile = receivingFile;
	}

	public ReceivingFile getReceivingFile() {
		return receivingFile;
	}

	@Override
	protected SocketChannel getChannel() {
		return fileChannel;
	}

	@Override
	public boolean isFileConnection() {
		return true;
	}

	@Override
	public void close() throws Exception {
		fileChannel.close();
	}
}
