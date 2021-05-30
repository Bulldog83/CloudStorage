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
	private String arguments = "";

	ServerCommand(String name, String desc) {
		this.name = name;
		this.description = desc;
	}

	public String getArguments() {
		return arguments;
	}

	private void setArguments(String arguments) {
		this.arguments = arguments;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public static Optional<ServerCommand> of(String data) {
		for (ServerCommand command : values()) {
			String commandName = command.getName();
			if (data != null && data.startsWith(commandName)) {
				String arguments = data.replaceAll(commandName + " ", "");
				command.setArguments(arguments);
				return Optional.of(command);
			}
		}
		return Optional.empty();
	}
}
