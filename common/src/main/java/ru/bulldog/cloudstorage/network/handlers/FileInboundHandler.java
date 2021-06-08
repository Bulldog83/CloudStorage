package ru.bulldog.cloudstorage.network.handlers;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.stream.ChunkedFile;

public abstract class FileInboundHandler extends SimpleChannelInboundHandler<ChunkedFile> {}
