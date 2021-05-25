package ru.bulldog.cloudstorage.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection implements Connection {

	private final static Logger LOGGER = LogManager.getLogger(ServerConnection.class);

	private final NetworkHandler networkHandler;
	private final Socket connection;
	private final ObjectOutputStream outputStream;
	private final ObjectInputStream inputStream;

	public ServerConnection(ClientNetworkHandler networkHandler, Socket connection) throws IOException {
		this.networkHandler = networkHandler;
		this.connection = connection;
		this.outputStream = new ObjectOutputStream(connection.getOutputStream());
		this.inputStream = new ObjectInputStream(connection.getInputStream());
	}

	public void listen() {
		new Thread(() -> {
			try {
				while (isConnected()) {
					Packet packet = (Packet) inputStream.readObject();
					networkHandler.handlePacket(this, packet);
				}
			} catch (Exception ex) {
				LOGGER.error(ex.getLocalizedMessage(), ex);
			}
		}).start();
	}

	@Override
	public void sendData(Packet packet) throws IOException {
		if (isConnected()) {
			outputStream.writeObject(packet);
			outputStream.flush();
		}
	}

	@Override
	public boolean isConnected() {
		return !connection.isClosed() && connection.isConnected();
	}

	@Override
	public void close() throws Exception {
		connection.close();
	}
}
