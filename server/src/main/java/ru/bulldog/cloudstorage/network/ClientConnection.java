package ru.bulldog.cloudstorage.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientConnection implements Connection {

	private final static Logger LOGGER = LogManager.getLogger(ClientConnection.class);

	private final NetworkHandler server;
	private final Socket socket;
	private final ObjectOutputStream outputStream;
	private final ObjectInputStream inputStream;

	public ClientConnection(ServerNetworkHandler server, Socket socket) throws IOException {
		this.server = server;
		this.socket = socket;
		this.outputStream = new ObjectOutputStream(socket.getOutputStream());
		this.inputStream = new ObjectInputStream(socket.getInputStream());
	}

	public void listen() {
		new Thread(() -> {
			try {
				while (isConnected()) {
					Packet packet = (Packet) inputStream.readObject();
					server.handlePacket(this, packet);
				}
			} catch (SocketException sEx) {
				LOGGER.warn(sEx.getLocalizedMessage());
			} catch (Exception ex) {
				LOGGER.error(ex.getLocalizedMessage(), ex);
			}
		}).start();
	}

	public void sendData(Packet packet) throws IOException {
		if (isConnected()) {
			outputStream.writeObject(packet);
			outputStream.flush();
		}
	}

	@Override
	public void close() throws Exception {
		socket.close();
	}

	@Override
	public boolean isConnected() {
		return !socket.isClosed() && socket.isConnected();
	}
}
