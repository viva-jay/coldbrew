package v2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class TestHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(ClientToServerChannelHandler.class);

    private final String remoteHost;
    private final int remotePort;
    private final AtomicInteger connections;

    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.
//    private Channel outboundChannel;

    public TestHandler(String remoteHost, int remotePort, AtomicInteger connections) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.connections = connections;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
        final Channel inboundChannel = context.channel();
        connections.incrementAndGet();
        inboundChannel.read();

        // Start the connection attempt.
//        Bootstrap b = new Bootstrap();
//        b.group(inboundChannel.eventLoop())
//                .channel(context.channel().getClass())
//                .handler(new ClientToServerConnectionHandler(inboundChannel))
//                .option(KQueueChannelOption.AUTO_READ, false)
////                .option(ChannelOption.SO_LINGER,0)
////                .option(ChannelOption.SO_REUSEADDR, true)
////                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
////                .option(ChannelOption.SO_SNDBUF, 1024 * 1024)
//                .option(KQueueChannelOption.TCP_NODELAY,true);
//        ChannelFuture f = b.connect(remoteHost, remotePort);
//        outboundChannel = f.channel();
//        f.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) {
//                if (future.isSuccess()) {
//                    // connection complete start to read first data
//                    inboundChannel.read();
//                } else {
//                    // Close the connection if the connection attempt has failed.
//                    inboundChannel.close();
//                }
//            }
//        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, Object messsage) {
        final Channel inboundChannel = context.channel();
//        if (outboundChannel.isActive()) {
//            // 클라이언트에서 서버로 메시지를 보낸다(클라이언트로부터 받은 메시지)
//            outboundChannel.writeAndFlush(messsage).addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) {
//                    if (future.isSuccess()) {
//                        // was able to flush out data, start to read the next chunk
//                        context.channel().read();
//                    } else {
//                        future.channel().close();
//                    }
//                }
//            });
//        }

        inboundChannel.writeAndFlush(messsage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // was able to flush out data, start to read the next chunk
                    context.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        Channel inboundChannel = context.channel();
        closeOnFlush(inboundChannel);
//        if (outboundChannel != null) {
//            closeOnFlush(outboundChannel);
//        }


//        logger.info("active conntection count {}.",connections.decrementAndGet());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(context.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel channel) {
        if (channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
