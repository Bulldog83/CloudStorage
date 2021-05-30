package ru.bulldog.cloudstorage.network;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.stream.ChunkedFile;

public abstract class FileInboundHandler extends SimpleChannelInboundHandler<ChunkedFile> {}
