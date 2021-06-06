package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import ru.bulldog.cloudstorage.network.packet.Packet;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public class Connection {

	public final static AttributeKey<Connection> SESSION_KEY = AttributeKey.valueOf("client_session");

	private final Map<UUID, Channel> fileChannels = Maps.newHashMap();
	private final SocketChannel channel;
	protected UUID sessionId;

	public Connection(SocketChannel channel) {
		this.sessionId = UUID.randomUUID();
		this.channel = channel;
	}

	public Connection(SocketChannel channel, UUID sessionId) {
		this.sessionId = sessionId;
		this.channel = channel;
	}

	public final void registerChannel(UUID uuid, Channel channel) {
		fileChannels.put(uuid, channel);
	}

	@Nullable
	public final ChannelFuture closeChannel(UUID uuid) {
		if (fileChannels.containsKey(uuid)) {
			return fileChannels.remove(uuid).close();
		}
		return null;
	}

	protected SocketChannel getChannel() {
		return channel;
	}

	public void setSessionId(UUID uuid) {
		this.sessionId = uuid;
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
