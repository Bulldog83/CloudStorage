package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelOutboundInvoker;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Session {

	public final Map<ChannelId, Channel> fileChannels = Maps.newHashMap();
	public final UUID sessionId;
	public final Channel channel;

	public Session(UUID sessionId, Channel channel) {
		this.sessionId = sessionId;
		this.channel = channel;
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public Channel getChannel() {
		return channel;
	}

	public void registerFileChannel(Channel channel) {
		fileChannels.put(channel.id(), channel);
	}

	public Optional<ChannelFuture> closeChannel(ChannelId id) {
		if (fileChannels.containsKey(id)) {
			return Optional.of(fileChannels.remove(id).close());
		}
		return Optional.empty();
	}

	public ChannelFuture close() {
		fileChannels.values().forEach(ChannelOutboundInvoker::close);
		return channel.close();
	}
}
