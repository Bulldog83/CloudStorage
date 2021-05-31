package ru.bulldog.cloudstorage.network;

import io.netty.channel.socket.SocketChannel;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.util.Optional;

public class Connection implements AutoCloseable {

	private final SocketChannel channel;
	private ReceivingFile receivingFile;

	public Connection(SocketChannel channel) {
		this.channel = channel;
	}

	public void sendPacket(Packet packet) {
		channel.writeAndFlush(packet);
	}

	public Optional<ReceivingFile> getReceivingFile() {
		return Optional.ofNullable(receivingFile);
	}

	public boolean isReceiving() {
		return receivingFile != null;
	}

	public void setReceivingFile(ReceivingFile file) {
		this.receivingFile = file;
	}

	public void fileReceived() {
		this.receivingFile = null;
	}

	public boolean isConnected() {
		return channel.isOpen() && channel.isActive();
	}

	@Override
	public void close() throws Exception {
		channel.close();
	}
}
