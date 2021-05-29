package ru.bulldog.cloudstorage.command;

import ru.bulldog.cloudstorage.network.ServerNetworkHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class ServerCommands {
	private final static String SUCCESS = "Success\n\r";
	private final static String FAIL = "Fail: ";

	private final ServerNetworkHandler server;

	public ServerCommands(ServerNetworkHandler server) {
		this.server = server;
	}

	public byte[] execute(ServerCommand command) throws Exception {
		switch (command) {
			case HALT:
				System.exit(0);
				return success();
			case LS:
				return getDirList();
			case CAT:
				return getFileData(command.getData());
			case READ:
				return writeString(command.getData());
			case MKDIR:
				return makeDir(command.getData());
			case TOUCH:
				return createFile(command.getData());
			case HELP:
				return printHelp();
			default:
				return new byte[0];
		}
	}

	private byte[] printHelp() {
		StringBuilder builder = new StringBuilder();
		builder.append("Help:\n\r");
		Arrays.stream(ServerCommand.values()).forEach(command -> {
			builder.append("\t")
					.append(command.getName())
					.append("\n\t\t")
					.append(command.getDescription())
					.append("\n\r");
		});
		return builder.toString().getBytes(StandardCharsets.UTF_8);
	}

	private byte[] writeString(String data) {
		String[] params = data.split(" > ");
		if (params.length < 2) {
			return fail("Invalid command params.");
		}
		try {
			Path file = server.getFilesDir().resolve(params[1]);
			if (Files.isWritable(file)) {
				byte[] bytes = params[0].getBytes(StandardCharsets.UTF_8);
				Files.write(file, bytes, StandardOpenOption.APPEND);
				return success();
			}
			return fail("Can't write data into file " + params[1]);
		} catch (Exception ex) {
			return fail(ex.getMessage());
		}
	}

	private byte[] createFile(String path) {
		Path file = server.getFilesDir().resolve(path);
		try {
			if (file.toFile().createNewFile()) {
				return success();
			}
			return fail("Can't create file: " + path);
		} catch (Exception ex) {
			return fail(ex.getMessage());
		}
	}

	private byte[] getDirList() throws IOException {
		Path dir = server.getFilesDir();
		StringBuilder builder = new StringBuilder();
		builder.append("Files:\n\r");
		Files.list(dir).forEach(file -> {
			builder.append("\t")
					.append(file.getFileName().toString())
					.append("\n\r");
		});
		return builder.toString().getBytes(StandardCharsets.UTF_8);
	}

	private byte[] getFileData(String name) {
		try {
			Path file = server.getFilesDir().resolve(name);
			return Files.readAllBytes(file);
		} catch (Exception ex) {
			return fail(ex.getMessage());
		}
	}

	private byte[] makeDir(String path) {
		try {
			Path dir = server.getFilesDir().resolve(path);
			if (dir.toFile().mkdirs()) {
				return success();
			}
			return fail("Can't create directory: " + path);
		} catch (Exception ex) {
			return fail(ex.getMessage());
		}
	}

	private byte[] fail(String cause) {
		return (FAIL + cause + "\n\r").getBytes(StandardCharsets.UTF_8);
	}

	private byte[] success() {
		return SUCCESS.getBytes(StandardCharsets.UTF_8);
	}
}
