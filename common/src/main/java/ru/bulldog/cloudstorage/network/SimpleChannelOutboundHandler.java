package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

public abstract class SimpleChannelOutboundHandler<I> extends ChannelOutboundHandlerAdapter {

	private final TypeParameterMatcher matcher;
	private final boolean autoRelease;

	protected SimpleChannelOutboundHandler() {
		this(true);
	}

	protected SimpleChannelOutboundHandler(boolean autoRelease) {
		this.matcher = TypeParameterMatcher.find(this, SimpleChannelOutboundHandler.class, "I");
		this.autoRelease = autoRelease;
	}

	protected SimpleChannelOutboundHandler(Class<? extends I> inboundMessageType) {
		this(inboundMessageType, true);
	}

	protected SimpleChannelOutboundHandler(Class<? extends I> inboundMessageType, boolean autoRelease) {
		this.matcher = TypeParameterMatcher.get(inboundMessageType);
		this.autoRelease = autoRelease;
	}

	public boolean acceptInboundMessage(Object msg) throws Exception {
		return matcher.match(msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		boolean release = true;
		try {
			if (acceptInboundMessage(msg)) {
				@SuppressWarnings("unchecked")
				I imsg = (I) msg;
				write0(ctx, imsg, promise);
			} else {
				release = false;
				super.write(ctx, msg, promise);
			}
		} finally {
			if (autoRelease && release) {
				ReferenceCountUtil.release(msg);
			}
		}
	}

	public abstract void write0(ChannelHandlerContext ctx, I msg, ChannelPromise promise) throws Exception;
}
