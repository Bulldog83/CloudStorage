package ru.bulldog.cloudstorage.network;

import io.netty.channel.Channel;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.util.UUID;

public class FileConnection extends Connection {

	private final ReceivingFile receivingFile;
	private final UUID sessionId;

	public FileConnection(UUID sessionId, Channel channel, ReceivingFile receivingFile) {
		super(channel);
		this.sessionId = sessionId;
		this.receivingFile = receivingFile;
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public ReceivingFile getReceivingFile() {
		return receivingFile;
	}

	@Override
	public boolean isFileConnection() {
		return true;
	}
}
