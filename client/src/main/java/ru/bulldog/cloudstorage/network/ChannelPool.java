package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.tasks.NamedTask;
import ru.bulldog.cloudstorage.tasks.ThreadManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChannelPool implements AutoCloseable {

	private static final Logger logger = LogManager.getLogger(ChannelPool.class);

	private final List<Channel> channels = Lists.newCopyOnWriteArrayList();
	private final ThreadManager threadManager;
	private final Bootstrap bootstrap;
	private final int count;

	public ChannelPool(ThreadManager threadManager, Bootstrap bootstrap, int channelsCount) {
		this.threadManager = threadManager;
		this.bootstrap = bootstrap;
		this.count = channelsCount;
	}

	public Channel openChannel() {
		CompletableFuture<Channel> futureChannel = new CompletableFuture<>();
		threadManager.execute(new NamedTask("ChannelWaiting", () -> {
			try {
				while (!futureChannel.isDone()) {
					if (channels.size() < count) {
						Channel channel = connect();
						futureChannel.complete(channel);
						break;
					}
				}
			} catch (Exception ex) {
				logger.error("Open channel error", ex);
				futureChannel.completeExceptionally(ex);
			}
		}, false));
		return futureChannel.join();
	}

	private Channel connect() {
		CompletableFuture<Channel> futureChannel = new CompletableFuture<>();
		threadManager.execute(new NamedTask("ChannelThread", () -> {
			try {
				ChannelFuture channelFuture = bootstrap.connect().sync();
				Channel channel = channelFuture.channel();
				futureChannel.complete(channel);
				logger.debug("Channel created: " + channel);
				channels.add(channel);
				channel.closeFuture().addListener(future -> {
					if (future.isDone()) {
						logger.debug("Channel closed: " + channel);
						channels.remove(channel);
					}
				}).sync();
			} catch (Exception ex) {
				logger.error("Connection error.", ex);
				futureChannel.completeExceptionally(ex);
			}
		}, true));
		return futureChannel.join();
	}

	@Override
	public void close() throws Exception {
		channels.forEach(channel -> {
			if (channel.isOpen()) channel.close();
		});
	}
}
