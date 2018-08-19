package com.chenfq.wschatroom;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class WebSocketServer {
    private int port;

    public WebSocketServer(int port) {
        this.port = port;
    }

    public void run()throws Exception{
        EventLoopGroup boosGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boosGroup,workGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new WebSocketChatServerInitializer())
                    .option(ChannelOption.SO_KEEPALIVE,true);

            System.out.println("WebSocketChatServer 启动了");

            ChannelFuture future = b.bind(port).sync();

            future.channel().closeFuture().sync();

        }finally {
            workGroup.shutdownGracefully();
            boosGroup.shutdownGracefully();
            System.out.println("WebSocketChatServer 关闭了");

        }
    }

    public static void main(String[] args) throws Exception{
        int port;
        if (args.length > 0){
            port = Integer.parseInt(args[0]);
        }else {
            port = 8080;
        }
        new WebSocketServer(port).run();
    }
}
