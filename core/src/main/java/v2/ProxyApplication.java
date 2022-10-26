package v2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.kqueue.KQueueChannelOption;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;

public class ProxyApplication {
    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "80"));
    static final String REMOTE_HOST = System.getProperty("remoteHost", "mock-http-1");
    static final int REMOTE_PORT = Integer.parseInt(System.getProperty("remotePort", "80"));

    public static void main(String[] args) throws Exception {
        int cpu = Runtime.getRuntime().availableProcessors();
        EventLoopGroup bossGroup = new KQueueEventLoopGroup(cpu);
        EventLoopGroup workerGroup = new KQueueEventLoopGroup(cpu * 2);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(KQueueServerSocketChannel.class)
//                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .option(KQueueChannelOption.SO_REUSEPORT, true)

//                  .option(KQueueChannelOption.ALLOW_HALF_CLOSURE,)
//            09:54:06.214 [KQueueEventLoopGroup-3-2] INFO v2.ProxyServerInitializer - rcc buf 407800
//            09:54:06.214 [KQueueEventLoopGroup-3-2] INFO v2.ProxyServerInitializer - snd buf 146808
//            09:54:06.215 [KQueueEventLoopGroup-3-2] INFO v2.ProxyServerInitializer - time out 30000

//            05:04:15.062 [KQueueEventLoopGroup-3-4] INFO v2.ServerToClientConnectionHandler - snd 146988  rcv 408300
//            05:04:15.062 [KQueueEventLoopGroup-3-5] INFO v2.ClientToServerChannelHandler - snd 131072  rcv 131072

                    .childHandler(new ProxyServerInitializer(REMOTE_HOST, new int[]{8081, 8082}))
                    .childOption(KQueueChannelOption.AUTO_READ, false)
                    .childOption(ChannelOption.SO_KEEPALIVE,false)
                    .childOption(ChannelOption.SO_RCVBUF, 65536)
                    .childOption(ChannelOption.SO_SNDBUF,65536)
//                    .childOption(ChannelOption.)
//                    .childOption(KQueueChannelOption.TCP_NODELAY, true)
                    .bind(LOCAL_PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
