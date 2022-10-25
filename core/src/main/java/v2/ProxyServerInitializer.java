package v2;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class ProxyServerInitializer extends ChannelInitializer<SocketChannel> {
    Logger logger = LoggerFactory.getLogger(ProxyServerInitializer.class);
    private final AtomicInteger connections = new AtomicInteger();
    private final String remoteHost;
    private final PortLoadBalancer loadBalancer;

    public ProxyServerInitializer(String remoteHost, int[] remotePort) {
        this.remoteHost = remoteHost;
        this.loadBalancer = new PortLoadBalancer(remotePort);
    }

    @Override
    public void initChannel(SocketChannel channel) {
//        logger.info("rcc buf {}", channel.config().getReceiveBufferSize());
//        logger.info("snd buf {}", channel.config().getSendBufferSize());
//        logger.info("time out {}", channel.config().getConnectTimeoutMillis());



        channel.pipeline().addLast(
//                new LoggingHandler(LogLevel.DEBUG),
                new ClientToServerChannelHandler(remoteHost, loadBalancer.getNext(), connections));
//                new TestHandler(remoteHost, loadBalancer.getNext(), connections));
    }
}
