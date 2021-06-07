package ru.bulldog.cloudstorage.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import ru.bulldog.cloudstorage.network.packet.Packet;

public class Connection {

	private final Channel channel;

	public Connection(Channel channel) {
		this.channel = channel;
	}

	protected Channel getChannel() {
		return channel;
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
