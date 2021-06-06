package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.util.UUID;

public class FileConnection extends Connection {

	private final ReceivingFile receivingFile;
	private final SocketChannel fileChannel;
	private final UUID channelId;

	public FileConnection(UUID channelId, Connection connection, SocketChannel channel, ReceivingFile receivingFile) {
		super(connection.getChannel(), connection.getUUID());
		this.channelId = channelId;
		this.fileChannel = channel;
		this.receivingFile = receivingFile;
	}

	public UUID getChannelId() {
		return channelId;
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
	public ChannelFuture close() {
		return fileChannel.close();
	}
}
