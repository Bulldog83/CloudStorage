package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.FileOutputStream;

public class FileInboundHandler extends SimpleChannelInboundHandler<ChunkedFile> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ChunkedFile chunkedFile) throws Exception {
		ctx.writeAndFlush(chunkedFile);
		try (FileOutputStream fos = new FileOutputStream("test.")) {
			while (!chunkedFile.isEndOfInput()) {
				ByteBuf buffer = chunkedFile.readChunk(ctx.alloc());
			}
		}
	}
}
