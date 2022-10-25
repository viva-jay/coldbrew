package v2;

import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerToClientConnectionHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(ServerToClientConnectionHandler.class);
    private final Channel inboundChannel;

    public ServerToClientConnectionHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
//        logger.info("snd {}  rcv {}", context.channel().config().getOptions().get(ChannelOption.SO_SNDBUF)
//                , context.channel().config().getOptions().get(ChannelOption.SO_RCVBUF));

        context.read();
    }

    // 버퍼에 읽을 데이터가 남아있다면 while으로 반복하며 계속 channelRead 이벤트가 발생한다.
    // 버퍼가 크면 읽을 데이터가 많기 때문에 channelRead가 실행되는 횟수가 많을것 같다.
    @Override
    public void channelRead(final ChannelHandlerContext context, Object messsage) {
        // 서버에서 클라이언트로 보내는 메시지(프록시는 서버로 부터 메시지를 수신했다.
        inboundChannel.writeAndFlush(messsage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    context.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        // 서버와 연결된 채널이 비활성화 되면 인바운트채널(클라이언트와 프록시가 연결된 채널)을 닫는다.
        ClientToServerChannelHandler.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        ClientToServerChannelHandler.closeOnFlush(context.channel());
    }
}
