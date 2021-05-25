package ru.bulldog.cloudstorage.command;

import java.util.Optional;

public enum ServerCommand {
	HALT("halt", "Stop server."),
	LS("ls", "List directory files."),
	CAT("cat", "Print file. Usage: cat <name>"),
	MKDIR("mkdir", "Create directory. Usage: mkdir <name>"),
	TOUCH("touch", "Create empty file. Usage: touch <name>"),
	READ("read", "Write text into file. Usage: read \"message\" > <name>"),
	HELP("help", "Print commands list.");

	private final String name;
	private final String description;
	private String data = "";

	ServerCommand(String name, String desc) {
		this.name = name;
		this.description = desc;
	}

	public String getData() {
		return data;
	}

	private void setData(String data) {
		this.data = data.replaceAll(getName() + " ", "");
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public static Optional<ServerCommand> of(String data) {
		for (ServerCommand command : values()) {
			if (data != null && data.startsWith(command.getName())) {
				command.setData(data);
				return Optional.of(command);
			}
		}
		return Optional.empty();
	}
}
