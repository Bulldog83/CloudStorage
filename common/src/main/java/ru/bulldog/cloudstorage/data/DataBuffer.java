package ru.bulldog.cloudstorage.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ByteProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DataBuffer extends ByteBuf {

	private final ByteBuf bufferInternal;

	public DataBuffer(ByteBufAllocator allocator) {
		this.bufferInternal = allocator.buffer();
	}

	public DataBuffer(ByteBufAllocator allocator, int capacity) {
		this(allocator);
		bufferInternal.capacity(capacity);
	}

	public DataBuffer(ByteBuf source) {
		this.bufferInternal = source;
	}

	public String readString() {
		int len = readInt();
		byte[] bytes = new byte[len];
		readBytes(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public DataBuffer writeString(String str) {
		writeInt(str.length());
		writeBytes(str.getBytes(StandardCharsets.UTF_8));
		return this;
	}

	public DataBuffer writeUUID(UUID uuid) {
		writeLong(uuid.getMostSignificantBits());
		writeLong(uuid.getLeastSignificantBits());
		return this;
	}

	public UUID readUUID() {
		long mostBits = readLong();
		long leastBits = readLong();
		return new UUID(mostBits, leastBits);
	}

	public DataBuffer merge(ByteBuf source) {
		capacity(capacity() + source.capacity());
		writeBytes(source);
		return this;
	}

	@Override
	public int capacity() {
		return bufferInternal.capacity();
	}

	@Override
	public ByteBuf capacity(int newCapacity) {
		bufferInternal.capacity(newCapacity);
		return this;
	}

	@Override
	public int maxCapacity() {
		return bufferInternal.maxCapacity();
	}

	@Override
	public ByteBufAllocator alloc() {
		return bufferInternal.alloc();
	}

	@Override
	@Deprecated
	public ByteOrder order() {
		return bufferInternal.order();
	}

	@Override
	@Deprecated
	public ByteBuf order(ByteOrder endianness) {
		if (endianness == order()) {
			return this;
		}
		return bufferInternal.order(endianness);
	}

	@Override
	public ByteBuf unwrap() {
		return bufferInternal;
	}

	@Override
	public boolean isDirect() {
		return bufferInternal.isDirect();
	}

	@Override
	public boolean isReadOnly() {
		return bufferInternal.isReadOnly();
	}

	@Override
	public ByteBuf asReadOnly() {
		return bufferInternal.asReadOnly();
	}

	@Override
	public int readerIndex() {
		return bufferInternal.readerIndex();
	}

	@Override
	public ByteBuf readerIndex(int readerIndex) {
		bufferInternal.readerIndex(readerIndex);
		return this;
	}

	@Override
	public int writerIndex() {
		return bufferInternal.writerIndex();
	}

	@Override
	public ByteBuf writerIndex(int writerIndex) {
		bufferInternal.writerIndex(writerIndex);
		return this;
	}

	@Override
	public ByteBuf setIndex(int readerIndex, int writerIndex) {
		bufferInternal.setIndex(readerIndex, writerIndex);
		return this;
	}

	@Override
	public int readableBytes() {
		return bufferInternal.readableBytes();
	}

	@Override
	public int writableBytes() {
		return bufferInternal.writableBytes();
	}

	@Override
	public int maxWritableBytes() {
		return bufferInternal.maxWritableBytes();
	}

	@Override
	public boolean isReadable() {
		return bufferInternal.isReadable();
	}

	@Override
	public boolean isReadable(int size) {
		return bufferInternal.isReadable(size);
	}

	@Override
	public boolean isWritable() {
		return bufferInternal.isWritable();
	}

	@Override
	public boolean isWritable(int size) {
		return bufferInternal.isWritable(size);
	}

	@Override
	public ByteBuf clear() {
		return bufferInternal.clear();
	}

	@Override
	public ByteBuf markReaderIndex() {
		bufferInternal.markReaderIndex();
		return this;
	}

	@Override
	public ByteBuf resetReaderIndex() {
		bufferInternal.resetReaderIndex();
		return this;
	}

	@Override
	public ByteBuf markWriterIndex() {
		bufferInternal.markWriterIndex();
		return this;
	}

	@Override
	public ByteBuf resetWriterIndex() {
		bufferInternal.resetWriterIndex();
		return this;
	}

	@Override
	public ByteBuf discardReadBytes() {
		bufferInternal.discardReadBytes();
		return this;
	}

	@Override
	public ByteBuf discardSomeReadBytes() {
		bufferInternal.discardSomeReadBytes();
		return this;
	}

	@Override
	public ByteBuf ensureWritable(int minWritableBytes) {
		bufferInternal.ensureWritable(minWritableBytes);
		return this;
	}

	@Override
	public int ensureWritable(int minWritableBytes, boolean force) {
		return bufferInternal.ensureWritable(minWritableBytes, force);
	}

	@Override
	public boolean getBoolean(int index) {
		return bufferInternal.getBoolean(index);
	}

	@Override
	public byte getByte(int index) {
		return bufferInternal.getByte(index);
	}

	@Override
	public short getUnsignedByte(int index) {
		return bufferInternal.getUnsignedByte(index);
	}

	@Override
	public short getShort(int index) {
		return bufferInternal.getShort(index);
	}

	@Override
	public short getShortLE(int index) {
		return bufferInternal.getShortLE(index);
	}

	@Override
	public int getUnsignedShort(int index) {
		return bufferInternal.getUnsignedShort(index);
	}

	@Override
	public int getUnsignedShortLE(int index) {
		return bufferInternal.getUnsignedShortLE(index);
	}

	@Override
	public int getMedium(int index) {
		return bufferInternal.getMedium(index);
	}

	@Override
	public int getMediumLE(int index) {
		return bufferInternal.getMediumLE(index);
	}

	@Override
	public int getUnsignedMedium(int index) {
		return bufferInternal.getUnsignedMedium(index);
	}

	@Override
	public int getUnsignedMediumLE(int index) {
		return bufferInternal.getUnsignedMediumLE(index);
	}

	@Override
	public int getInt(int index) {
		return bufferInternal.getInt(index);
	}

	@Override
	public int getIntLE(int index) {
		return bufferInternal.getIntLE(index);
	}

	@Override
	public long getUnsignedInt(int index) {
		return bufferInternal.getUnsignedInt(index);
	}

	@Override
	public long getUnsignedIntLE(int index) {
		return bufferInternal.getUnsignedIntLE(index);
	}

	@Override
	public long getLong(int index) {
		return bufferInternal.getLong(index);
	}

	@Override
	public long getLongLE(int index) {
		return bufferInternal.getLongLE(index);
	}

	@Override
	public char getChar(int index) {
		return bufferInternal.getChar(index);
	}

	@Override
	public float getFloat(int index) {
		return bufferInternal.getFloat(index);
	}

	@Override
	public double getDouble(int index) {
		return bufferInternal.getDouble(index);
	}

	@Override
	public ByteBuf getBytes(int index, ByteBuf dst) {
		bufferInternal.getBytes(index, dst);
		return this;
	}

	@Override
	public ByteBuf getBytes(int index, ByteBuf dst, int length) {
		bufferInternal.getBytes(index, dst, length);
		return this;
	}

	@Override
	public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
		bufferInternal.getBytes(index, dst, dstIndex, length);
		return this;
	}

	@Override
	public ByteBuf getBytes(int index, byte[] dst) {
		bufferInternal.getBytes(index, dst);
		return this;
	}

	@Override
	public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
		bufferInternal.getBytes(index, dst, dstIndex, length);
		return this;
	}

	@Override
	public ByteBuf getBytes(int index, ByteBuffer dst) {
		bufferInternal.getBytes(index, dst);
		return this;
	}

	@Override
	public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
		bufferInternal.getBytes(index, out, length);
		return this;
	}

	@Override
	public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
		return bufferInternal.getBytes(index, out, length);
	}

	@Override
	public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
		return bufferInternal.getBytes(index, out, position, length);
	}

	@Override
	public CharSequence getCharSequence(int index, int length, Charset charset) {
		return bufferInternal.getCharSequence(index, length, charset);
	}

	@Override
	public ByteBuf setBoolean(int index, boolean value) {
		bufferInternal.setBoolean(index, value);
		return this;
	}

	@Override
	public ByteBuf setByte(int index, int value) {
		bufferInternal.setByte(index, value);
		return this;
	}

	@Override
	public ByteBuf setShort(int index, int value) {
		bufferInternal.setShort(index, value);
		return this;
	}

	@Override
	public ByteBuf setShortLE(int index, int value) {
		bufferInternal.setShortLE(index, value);
		return this;
	}

	@Override
	public ByteBuf setMedium(int index, int value) {
		bufferInternal.setMedium(index, value);
		return this;
	}

	@Override
	public ByteBuf setMediumLE(int index, int value) {
		bufferInternal.setMediumLE(index, value);
		return this;
	}

	@Override
	public ByteBuf setInt(int index, int value) {
		bufferInternal.setIndex(index, value);
		return this;
	}

	@Override
	public ByteBuf setIntLE(int index, int value) {
		bufferInternal.setIntLE(index, value);
		return this;
	}

	@Override
	public ByteBuf setLong(int index, long value) {
		bufferInternal.setLong(index, value);
		return this;
	}

	@Override
	public ByteBuf setLongLE(int index, long value) {
		bufferInternal.setLongLE(index, value);
		return this;
	}

	@Override
	public ByteBuf setChar(int index, int value) {
		bufferInternal.setChar(index, value);
		return this;
	}

	@Override
	public ByteBuf setFloat(int index, float value) {
		bufferInternal.setFloat(index, value);
		return this;
	}

	@Override
	public ByteBuf setDouble(int index, double value) {
		bufferInternal.setDouble(index, value);
		return this;
	}

	@Override
	public ByteBuf setBytes(int index, ByteBuf src) {
		bufferInternal.setBytes(index, src);
		return this;
	}

	@Override
	public ByteBuf setBytes(int index, ByteBuf src, int length) {
		bufferInternal.setBytes(index, src, length);
		return this;
	}

	@Override
	public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
		bufferInternal.setBytes(index, src, srcIndex, length);
		return this;
	}

	@Override
	public ByteBuf setBytes(int index, byte[] src) {
		bufferInternal.setBytes(index, src);
		return this;
	}

	@Override
	public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
		bufferInternal.setBytes(index, src, srcIndex, length);
		return this;
	}

	@Override
	public ByteBuf setBytes(int index, ByteBuffer src) {
		bufferInternal.setBytes(index, src);
		return this;
	}

	@Override
	public int setBytes(int index, InputStream in, int length) throws IOException {
		return bufferInternal.setBytes(index, in, length);
	}

	@Override
	public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
		return bufferInternal.setBytes(index, in, length);
	}

	@Override
	public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
		return bufferInternal.setBytes(index, in, position, length);
	}

	@Override
	public ByteBuf setZero(int index, int length) {
		bufferInternal.setZero(index, length);
		return this;
	}

	@Override
	public int setCharSequence(int index, CharSequence sequence, Charset charset) {
		return bufferInternal.setCharSequence(index, sequence, charset);
	}

	@Override
	public boolean readBoolean() {
		return bufferInternal.readBoolean();
	}

	@Override
	public byte readByte() {
		return bufferInternal.readByte();
	}

	@Override
	public short readUnsignedByte() {
		return bufferInternal.readUnsignedByte();
	}

	@Override
	public short readShort() {
		return bufferInternal.readShort();
	}

	@Override
	public short readShortLE() {
		return bufferInternal.readShortLE();
	}

	@Override
	public int readUnsignedShort() {
		return bufferInternal.readUnsignedShort();
	}

	@Override
	public int readUnsignedShortLE() {
		return bufferInternal.readUnsignedShortLE();
	}

	@Override
	public int readMedium() {
		return bufferInternal.readMedium();
	}

	@Override
	public int readMediumLE() {
		return bufferInternal.readMediumLE();
	}

	@Override
	public int readUnsignedMedium() {
		return bufferInternal.readUnsignedMedium();
	}

	@Override
	public int readUnsignedMediumLE() {
		return bufferInternal.readUnsignedMediumLE();
	}

	@Override
	public int readInt() {
		return bufferInternal.readInt();
	}

	@Override
	public int readIntLE() {
		return bufferInternal.readIntLE();
	}

	@Override
	public long readUnsignedInt() {
		return bufferInternal.readUnsignedInt();
	}

	@Override
	public long readUnsignedIntLE() {
		return bufferInternal.readUnsignedIntLE();
	}

	@Override
	public long readLong() {
		return bufferInternal.readLong();
	}

	@Override
	public long readLongLE() {
		return bufferInternal.readLongLE();
	}

	@Override
	public char readChar() {
		return bufferInternal.readChar();
	}

	@Override
	public float readFloat() {
		return bufferInternal.readFloat();
	}

	@Override
	public double readDouble() {
		return bufferInternal.readDouble();
	}

	@Override
	public ByteBuf readBytes(int length) {
		return bufferInternal.readBytes(length);
	}

	@Override
	public ByteBuf readSlice(int length) {
		return bufferInternal.readSlice(length);
	}

	@Override
	public ByteBuf readRetainedSlice(int length) {
		return bufferInternal.readRetainedSlice(length);
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst) {
		bufferInternal.readBytes(dst);
		return this;
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst, int length) {
		bufferInternal.readBytes(dst, length);
		return this;
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
		bufferInternal.readBytes(dst, dstIndex, length);
		return this;
	}

	@Override
	public ByteBuf readBytes(byte[] dst) {
		bufferInternal.readBytes(dst);
		return this;
	}

	@Override
	public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
		bufferInternal.readBytes(dst, dstIndex, length);
		return this;
	}

	@Override
	public ByteBuf readBytes(ByteBuffer dst) {
		bufferInternal.readBytes(dst);
		return this;
	}

	@Override
	public ByteBuf readBytes(OutputStream out, int length) throws IOException {
		bufferInternal.readBytes(out, length);
		return this;
	}

	@Override
	public int readBytes(GatheringByteChannel out, int length) throws IOException {
		return bufferInternal.readBytes(out, length);
	}

	@Override
	public CharSequence readCharSequence(int length, Charset charset) {
		return bufferInternal.readCharSequence(length, charset);
	}

	@Override
	public int readBytes(FileChannel out, long position, int length) throws IOException {
		return bufferInternal.readBytes(out, position, length);
	}

	@Override
	public ByteBuf skipBytes(int length) {
		bufferInternal.skipBytes(length);
		return this;
	}

	@Override
	public ByteBuf writeBoolean(boolean value) {
		bufferInternal.writeBoolean(value);
		return this;
	}

	@Override
	public ByteBuf writeByte(int value) {
		bufferInternal.writeByte(value);
		return this;
	}

	@Override
	public ByteBuf writeShort(int value) {
		bufferInternal.writeShort(value);
		return this;
	}

	@Override
	public ByteBuf writeShortLE(int value) {
		bufferInternal.writeShortLE(value);
		return this;
	}

	@Override
	public ByteBuf writeMedium(int value) {
		bufferInternal.writeMedium(value);
		return this;
	}

	@Override
	public ByteBuf writeMediumLE(int value) {
		bufferInternal.writeMediumLE(value);
		return this;
	}

	@Override
	public ByteBuf writeInt(int value) {
		bufferInternal.writeInt(value);
		return this;
	}

	@Override
	public ByteBuf writeIntLE(int value) {
		bufferInternal.writeIntLE(value);
		return this;
	}

	@Override
	public ByteBuf writeLong(long value) {
		bufferInternal.writeLong(value);
		return this;
	}

	@Override
	public ByteBuf writeLongLE(long value) {
		bufferInternal.writeLongLE(value);
		return this;
	}

	@Override
	public ByteBuf writeChar(int value) {
		bufferInternal.writeChar(value);
		return this;
	}

	@Override
	public ByteBuf writeFloat(float value) {
		bufferInternal.writeFloat(value);
		return this;
	}

	@Override
	public ByteBuf writeDouble(double value) {
		bufferInternal.writeDouble(value);
		return this;
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src) {
		bufferInternal.writeBytes(src);
		return this;
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src, int length) {
		bufferInternal.writeBytes(src, length);
		return this;
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
		bufferInternal.writeBytes(src, srcIndex, length);
		return this;
	}

	@Override
	public ByteBuf writeBytes(byte[] src) {
		bufferInternal.writeBytes(src);
		return this;
	}

	@Override
	public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
		bufferInternal.writeBytes(src, srcIndex, length);
		return this;
	}

	@Override
	public ByteBuf writeBytes(ByteBuffer src) {
		bufferInternal.writeBytes(src);
		return this;
	}

	@Override
	public int writeBytes(InputStream in, int length) throws IOException {
		return bufferInternal.writeBytes(in, length);
	}

	@Override
	public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
		return bufferInternal.writeBytes(in, length);
	}

	@Override
	public int writeBytes(FileChannel in, long position, int length) throws IOException {
		return bufferInternal.writeBytes(in, position, length);
	}

	@Override
	public ByteBuf writeZero(int length) {
		bufferInternal.writeZero(length);
		return this;
	}

	@Override
	public int writeCharSequence(CharSequence sequence, Charset charset) {
		return bufferInternal.writeCharSequence(sequence, charset);
	}

	@Override
	public int indexOf(int fromIndex, int toIndex, byte value) {
		return bufferInternal.indexOf(fromIndex, toIndex, value);
	}

	@Override
	public int bytesBefore(byte value) {
		return bufferInternal.bytesBefore(value);
	}

	@Override
	public int bytesBefore(int length, byte value) {
		return bufferInternal.bytesBefore(length, value);
	}

	@Override
	public int bytesBefore(int index, int length, byte value) {
		return bufferInternal.bytesBefore(index, length, value);
	}

	@Override
	public int forEachByte(ByteProcessor processor) {
		return bufferInternal.forEachByte(processor);
	}

	@Override
	public int forEachByte(int index, int length, ByteProcessor processor) {
		return bufferInternal.forEachByte(index, length, processor);
	}

	@Override
	public int forEachByteDesc(ByteProcessor processor) {
		return bufferInternal.forEachByteDesc(processor);
	}

	@Override
	public int forEachByteDesc(int index, int length, ByteProcessor processor) {
		return bufferInternal.forEachByteDesc(index, length, processor);
	}

	@Override
	public ByteBuf copy() {
		return new DataBuffer(bufferInternal.copy());
	}

	@Override
	public ByteBuf copy(int index, int length) {
		return new DataBuffer(bufferInternal.copy(index, length));
	}

	@Override
	public ByteBuf slice() {
		return bufferInternal.slice();
	}

	@Override
	public ByteBuf retainedSlice() {
		return bufferInternal.retainedSlice();
	}

	@Override
	public ByteBuf slice(int index, int length) {
		return bufferInternal.slice(index, length);
	}

	@Override
	public ByteBuf retainedSlice(int index, int length) {
		return bufferInternal.retainedSlice(index, length);
	}

	@Override
	public ByteBuf duplicate() {
		return bufferInternal.duplicate();
	}

	@Override
	public ByteBuf retainedDuplicate() {
		return bufferInternal.retainedDuplicate();
	}

	@Override
	public int nioBufferCount() {
		return bufferInternal.nioBufferCount();
	}

	@Override
	public ByteBuffer nioBuffer() {
		return bufferInternal.nioBuffer();
	}

	@Override
	public ByteBuffer nioBuffer(int index, int length) {
		return bufferInternal.nioBuffer(index, length);
	}

	@Override
	public ByteBuffer internalNioBuffer(int index, int length) {
		return bufferInternal.internalNioBuffer(index, length);
	}

	@Override
	public ByteBuffer[] nioBuffers() {
		return bufferInternal.nioBuffers();
	}

	@Override
	public ByteBuffer[] nioBuffers(int index, int length) {
		return bufferInternal.nioBuffers(index, length);
	}

	@Override
	public boolean hasArray() {
		return bufferInternal.hasArray();
	}

	@Override
	public byte[] array() {
		return bufferInternal.array();
	}

	@Override
	public int arrayOffset() {
		return bufferInternal.arrayOffset();
	}

	@Override
	public boolean hasMemoryAddress() {
		return bufferInternal.hasMemoryAddress();
	}

	@Override
	public long memoryAddress() {
		return bufferInternal.memoryAddress();
	}

	@Override
	public String toString(Charset charset) {
		return bufferInternal.toString(charset);
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		return bufferInternal.toString(index, length, charset);
	}

	@Override
	public int hashCode() {
		return bufferInternal.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ByteBuf)) return false;
		return bufferInternal.equals(obj);
	}

	@Override
	public int compareTo(ByteBuf buffer) {
		return bufferInternal.compareTo(buffer);
	}

	@Override
	public String toString() {
		return bufferInternal.toString();
	}

	@Override
	public ByteBuf retain(int increment) {
		bufferInternal.retain(increment);
		return this;
	}

	@Override
	public int refCnt() {
		return bufferInternal.refCnt();
	}

	@Override
	public ByteBuf retain() {
		bufferInternal.retain();
		return this;
	}

	@Override
	public ByteBuf touch() {
		bufferInternal.touch();
		return this;
	}

	@Override
	public ByteBuf touch(Object hint) {
		bufferInternal.touch(hint);
		return this;
	}

	@Override
	public boolean release() {
		return bufferInternal.release();
	}

	@Override
	public boolean release(int decrement) {
		return bufferInternal.release(decrement);
	}
}
