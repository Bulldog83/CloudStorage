package ru.bulldog.cloudstorage.network;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.util.Optional;
import java.util.UUID;

public class Session implements AutoCloseable {

	public final static AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("client_session");

	private final UUID sessionId;
	private final SocketChannel channel;
	private FileChannel fileChannel;
	private ReceivingFile receivingFile;

	public Session(SocketChannel channel) {
		this.sessionId = UUID.randomUUID();
		this.channel = channel;
	}

	public Session(SocketChannel channel, UUID sessionId) {
		this.sessionId = sessionId;
		this.channel = channel;
	}

	public UUID getUUID() {
		return sessionId;
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
