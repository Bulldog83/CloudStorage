package ru.bulldog.cloudstorage.network.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.TypeParameterMatcher;

public abstract class SimpleChannelOutboundHandler<I> extends ChannelOutboundHandlerAdapter {

	private final TypeParameterMatcher matcher;

	protected SimpleChannelOutboundHandler() {
		this.matcher = TypeParameterMatcher.find(this, SimpleChannelOutboundHandler.class, "I");
	}

	protected SimpleChannelOutboundHandler(Class<? extends I> inboundMessageType) {
		this.matcher = TypeParameterMatcher.get(inboundMessageType);
	}

	public boolean acceptInboundMessage(Object msg) throws Exception {
		return matcher.match(msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (acceptInboundMessage(msg)) {
			@SuppressWarnings("unchecked")
			I imsg = (I) msg;
			write0(ctx, imsg, promise);
		} else {
			super.write(ctx, msg, promise);
		}
	}

	public abstract void write0(ChannelHandlerContext ctx, I msg, ChannelPromise promise) throws Exception;
}
