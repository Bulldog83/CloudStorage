package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

import java.util.Arrays;

public class ActionPacket extends Packet {

	public static ActionPacket createFolder(String name) {
		return new ActionPacket(ActionType.FOLDER, name);
	}

	public static ActionPacket renameFile(String name, String newName) {
		return new ActionPacket(ActionType.RENAME, name, newName);
	}

	public static ActionPacket deleteFile(String name) {
		return new ActionPacket(ActionType.DELETE, name);
	}

	private final ActionType actionType;
	private final String name;
	private String newName;

	private ActionPacket(ActionType actionType, String name) {
		super(PacketType.ACTION);
		this.actionType = actionType;
		this.name = name;
	}

	private ActionPacket(ActionType actionType, String name, String newName) {
		this(actionType, name);
		this.newName = newName;
	}

	protected ActionPacket(DataBuffer buffer) {
		super(PacketType.ACTION);
		this.actionType = getActionType(buffer.readByte());
		this.name = buffer.readString();
		if (actionType == ActionType.RENAME) {
			this.newName = buffer.readString();
		}
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeByte(actionType.idx);
		buffer.writeString(name);
		if (actionType == ActionType.RENAME) {
			buffer.writeString(newName);
		}
	}

	public ActionType getActionType() {
		return actionType;
	}

	public String getName() {
		return name;
	}

	public String getNewName() {
		return newName;
	}

	private ActionType getActionType(byte idx) {
		return Arrays.stream(ActionType.values())
				.filter(actionType -> actionType.idx == idx)
				.findFirst().orElse(ActionType.UNKNOWN);
	}

	public enum ActionType {
		FOLDER(10),
		RENAME(11),
		DELETE(12),
		UNKNOWN(-1);

		private final byte idx;

		ActionType(int idx) {
			this.idx = (byte) idx;
		}
	}
}
