package v2;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.kqueue.KQueueChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientToServerChannelHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(ClientToServerChannelHandler.class);

    private final String remoteHost;
    private final int remotePort;
    private final AtomicInteger connections;
    private Instant start;

    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.
    private Channel outboundChannel;

    public ClientToServerChannelHandler(String remoteHost, int remotePort, AtomicInteger connections) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.connections = connections;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
//        logger.info("snd {}  rcv {}", context.channel().config().getOptions().get(ChannelOption.SO_SNDBUF)
//                , context.channel().config().getOptions().get(ChannelOption.SO_RCVBUF));
        final Channel inboundChannel = context.channel();
        connections.incrementAndGet();
        start = Instant.now();
        // Start the connection attempt.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(context.channel().getClass())
                .handler(new ServerToClientConnectionHandler(inboundChannel))
                .option(KQueueChannelOption.AUTO_READ, false)
                .option(ChannelOption.SO_KEEPALIVE,false)
                .option(ChannelOption.SO_RCVBUF, 65536)
                .option(ChannelOption.SO_SNDBUF,65536);
//                .option(KQueueChannelOption.ALLOW_HALF_CLOSURE, false)
//                .option(KQueueChannelOption.TCP_NODELAY, true);
        ChannelFuture f = b.connect(remoteHost, remotePort);
        outboundChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // connection complete start to read first data
                    inboundChannel.read();
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, Object messsage) {
        if (outboundChannel.isActive()) {
            // 클라이언트에서 서버로 메시지를 보낸다(클라이언트로부터 받은 메시지)
            outboundChannel.writeAndFlush(messsage).addListener(new ChannelFutureListener() {
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
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        // 하나의 채널은 약 30초간 지속되면서 데이터를 주고 받는다.
        // 따라서 channelActive() 이후 30초가 지나면 현재 메소드가 호출된다.
        /*
         * Keeps the TCP connection alive. If you do not need that feature
         * please uncomment the following commented line of code.
         */
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }

//       logger.info("running time {} ms.", Duration.between(start, Instant.now()).toMillis());
        logger.info("channel count {}.", connections.get());
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
