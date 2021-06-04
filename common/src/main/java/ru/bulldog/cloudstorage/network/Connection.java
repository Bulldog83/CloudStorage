package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.util.Optional;
import java.util.UUID;

public class Connection {

	public final static AttributeKey<Connection> SESSION_KEY = AttributeKey.valueOf("client_session");

	protected final UUID sessionId;
	private final SocketChannel channel;

	public Connection(SocketChannel channel) {
		this.sessionId = UUID.randomUUID();
		this.channel = channel;
	}

	public Connection(SocketChannel channel, UUID sessionId) {
		this.sessionId = sessionId;
		this.channel = channel;
	}

	protected SocketChannel getChannel() {
		return channel;
	}

	public UUID getUUID() {
		return sessionId;
	}

	public void sendPacket(Packet packet) {
		channel.writeAndFlush(packet);
	}

	public void sendMessage(String message) {
		channel.writeAndFlush(message);
	}

	public boolean isConnected() {
		return channel.isOpen() && channel.isActive();
	}

	public boolean isFileConnection() {
		return false;
	}

	public ChannelFuture close() {
		return channel.close();
	}
}
