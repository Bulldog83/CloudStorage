package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Session {

	private final Map<ChannelId, FileConnection> fileChannels = Maps.newHashMap();
	private final UUID sessionId;
	private final Connection connection;
	private Path activeFolder;
	private Path rootFolder;
	private boolean closed = false;

	public Session(UUID sessionId, Channel channel) {
		this.sessionId = sessionId;
		this.connection = new Connection(channel);
	}

	public Session(UUID sessionId, Connection connection) {
		this.sessionId = sessionId;
		this.connection = connection;
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

	public Path getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(Path rootFolder) {
		this.rootFolder = rootFolder;
	}

	public Path getActiveFolder() {
		return activeFolder;
	}

	public void setActiveFolder(Path activeFolder) {
		this.activeFolder = activeFolder;
	}

	public void addFileChannel(ChannelId channelId, FileConnection channel) {
		fileChannels.put(channelId, channel);
		channel.getChannel().closeFuture().addListener(future -> {
			if (future.isDone()) {
				fileChannels.remove(channelId);
			}
		});
	}

	public Optional<ChannelFuture> closeFileChannel(FileConnection connection) {
		ChannelId channelId = connection.getChannel().id();
		if (fileChannels.containsKey(channelId)) {
			return Optional.of(fileChannels.remove(channelId).close());
		}
		return Optional.empty();
	}

	public boolean isClosed() {
		return closed;
	}

	public ChannelFuture close() {
		this.closed = true;
		fileChannels.values().forEach(FileConnection::close);
		return connection.close();
	}

	public boolean isConnected() {
		return !closed && connection.isConnected();
	}

	public boolean isRegistered(Channel channel) {
		return sessionId.equals(channel.attr(ChannelAttributes.SESSION_KEY).get());
	}
}
