package ru.bulldog.cloudstorage.network;

import org.jetbrains.annotations.NotNull;

import java.nio.*;
import java.nio.charset.StandardCharsets;

public abstract class Packet {

	private final ByteBuffer buffer;

	public Packet(int capacity) {
		this.buffer = ByteBuffer.allocateDirect(capacity);
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public String getString() {
		int len = buffer.getInt();
		byte[] data = new byte[len];
		buffer.get(data);
		return new String(data, StandardCharsets.UTF_8);
	}

	@NotNull
	public Packet putString(String string) {
		byte[] data = string.getBytes(StandardCharsets.UTF_8);
		int len = data.length;
		buffer.putInt(len);
		buffer.put(data);
		return this;
	}

	public char getChar() {
		return buffer.getChar();
	}

	@NotNull
	public Packet putChar(char value) {
		buffer.putChar(value);
		return this;
	}

	public short getShort() {
		return buffer.getShort();
	}

	@NotNull
	public Packet putShort(short value) {
		buffer.putShort(value);
		return this;
	}

	public int getInt() {
		return buffer.getInt();
	}

	@NotNull
	public Packet putInt(int value) {
		buffer.putInt(value);
		return this;
	}

	public long getLong() {
		return buffer.getLong();
	}

	@NotNull
	public Packet putLong(long value) {
		buffer.putLong(value);
		return this;
	}

	public float getFloat() {
		return buffer.getFloat();
	}

	@NotNull
	public Packet putFloat(float value) {
		buffer.putFloat(value);
		return this;
	}

	public double getDouble() {
		return buffer.getDouble();
	}

	@NotNull
	public Packet putDouble(double value) {
		buffer.putDouble(value);
		return this;
	}
}
