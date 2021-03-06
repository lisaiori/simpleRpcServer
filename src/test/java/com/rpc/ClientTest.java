package com.rpc;

import com.shock.remote.protocol.RemoteMessage;
import com.shock.remote.server.HeartBeatHandler;
import com.shock.remote.server.RpcMessageProtoBufDecoder;
import com.shock.remote.server.RpcMessageProtoBufEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

/**
 * Created by shocklee on 16/6/27.
 */
public class ClientTest {

    /**
     * @param args
     */
    public static void main(String[] args) {

        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {

                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new LoggingHandler(LogLevel.ERROR));
                        p.addLast(new LengthFieldBasedFrameDecoder(65535,0,4,0,4));
                        p.addLast(new LengthFieldPrepender(4));
                        p.addLast(new RpcMessageProtoBufDecoder());
                        p.addLast(new RpcMessageProtoBufEncoder());
                        p.addLast(new HeartBeatHandler(300));
                        p.addLast(new HelloClientHandler());
                    }
                });
        ChannelFuture f = null;
        try {
            f = bootstrap.connect(new InetSocketAddress("127.0.0.1", 8081)).awaitUninterruptibly();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Wait until the connection is closed.
        try {
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class HelloClientHandler extends ChannelHandlerAdapter {

        int i =0;
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            RemoteMessage message = new RemoteMessage();
            for (int i=0;i<100;i++) {
                ctx.writeAndFlush(message);
            }
        }
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg){
            if(msg instanceof RemoteMessage){
                System.out.println(((RemoteMessage) msg).getMessageId());
                System.out.println(((RemoteMessage) msg).getRemarks());
                System.out.println(msg);
                System.out.println("消息index=" + (++i));
            }
        }

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            System.out.println("发生异常了");
            ctx.close();

        }
        @Override
        public void channelInactive(ChannelHandlerContext ctx)throws Exception{
            System.out.println("发生异常了");
            ctx.close();
        }
    }
}
