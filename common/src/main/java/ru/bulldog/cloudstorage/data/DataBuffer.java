package ru.bulldog.cloudstorage.data;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataBuffer {

	private byte[] data;

	public DataBuffer() {
		reset();
	}

	public byte[] getBytes() {
		return data.clone();
	}

	public void append(byte[] bytes) {
		int dl = data.length;
		int il = bytes.length;
		data = Arrays.copyOf(data, dl + il);
		System.arraycopy(bytes, 0, data, dl, il);
	}

	public void read(ByteBuffer buffer) {
		int len = buffer.remaining();
		int cl = data.length;
		data = Arrays.copyOf(data, len + cl);
		for (int i = 0; i < len; i++) {
			data[cl + i] = buffer.get();
		}
	}

	public void reset() {
		this.data = new byte[0];
	}
}
