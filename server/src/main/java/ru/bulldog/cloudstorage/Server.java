package ru.bulldog.cloudstorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.ServerNetworkHandler;

import java.util.Scanner;

public class Server {

	public final static Logger LOGGER = LogManager.getLogger(Server.class);

	public static void main(String[] args) {
		try(ServerNetworkHandler server = new ServerNetworkHandler()) {
			Scanner console = new Scanner(System.in);
			while (true) {
				if (console.hasNext()) {
					String line = console.nextLine();
					if (line.equals("/stop")) {
						server.close();
						break;
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.error(ex.getLocalizedMessage(), ex);
		}
	}
}
