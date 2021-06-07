package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Session {

	public final Map<ChannelId, FileConnection> fileChannels = Maps.newHashMap();
	public final UUID sessionId;
	public final Connection connection;

	public Session(UUID sessionId, Channel channel) {
		this.sessionId = sessionId;
		this.connection = new Connection(channel);
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public Channel getChannel() {
		return connection.getChannel();
	}

	public Connection getConnection() {
		return connection;
	}

	public FileConnection getFileChannel(ChannelId channelId) {
		return fileChannels.get(channelId);
	}

	public void addFileChannel(ChannelId channelId, FileConnection channel) {
		fileChannels.put(channelId, channel);
	}

	public Optional<ChannelFuture> closeFileChannel(ChannelId id) {
		if (fileChannels.containsKey(id)) {
			return Optional.of(fileChannels.remove(id).close());
		}
		return Optional.empty();
	}

	public ChannelFuture close() {
		fileChannels.values().forEach(FileConnection::close);
		return connection.close();
	}

	public boolean isConnected() {
		return connection.isConnected();
	}
}
