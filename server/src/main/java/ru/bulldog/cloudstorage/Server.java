package ru.bulldog.cloudstorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.ServerNetworkHandler;

import java.util.Properties;
import java.util.Scanner;

public class Server {

	public final static Logger LOGGER = LogManager.getLogger(Server.class);

	public static void main(String[] args) {
		Properties properties = System.getProperties();
		int port = (int) properties.getOrDefault("server.port", 8072);
		try {
			ServerNetworkHandler networkHandler = new ServerNetworkHandler(port);
			networkHandler.start();
			Scanner console = new Scanner(System.in);
			while (true) {
				if (console.hasNext()) {
					String line = console.nextLine();
					networkHandler.handleCommand(line, System.out);
				}
			}
		} catch (Exception ex) {
			LOGGER.error(ex.getLocalizedMessage(), ex);
		}
	}
}
