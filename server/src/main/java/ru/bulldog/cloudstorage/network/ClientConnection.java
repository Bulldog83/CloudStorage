package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Queues;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Queue;

public class ClientConnection implements Connection {

	private final static Logger logger = LogManager.getLogger(ClientConnection.class);

	private final Queue<File> waitingFiles = Queues.newConcurrentLinkedQueue();
	private final NetworkHandler networkHandler;
	private final Channel channel;
	private Path clientFolder;

	public ClientConnection(ServerNetworkHandler networkHandler, Channel channel) {
		this.networkHandler = networkHandler;
		this.channel = channel;
	}

	public boolean isWaitingFile() {
		return waitingFiles.size() > 0;
	}

	public Optional<File> getWaitingFile() {
		return Optional.ofNullable(waitingFiles.poll());
	}

	public void waitFile(File file) {
		waitingFiles.add(file);
	}

	@Override
	public void sendData(Packet packet) throws IOException {

	}

	@Override
	public void close() throws Exception {
		channel.close();
	}

	@Override
	public boolean isConnected() {
		return channel.isOpen() && channel.isActive();
	}
}
