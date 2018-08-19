package com.chenfq.wschatroom;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {

        Channel incoming = channelHandlerContext.channel();
        for (Channel channel : channels){
            if (channel != incoming){
                channel.writeAndFlush(new TextWebSocketFrame("["+incoming.remoteAddress()+"]"+textWebSocketFrame.text()));
            }else {
                channel.writeAndFlush(new TextWebSocketFrame("[you]"+textWebSocketFrame.text()));
            }
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        channels.writeAndFlush(new TextWebSocketFrame("[SERVER] - "+incoming.remoteAddress()+"加入"));
        channels.add(incoming);
        System.out.println("Client:"+incoming.remoteAddress()+"加入");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        channels.writeAndFlush(new TextWebSocketFrame("[SERVER] - "+incoming.remoteAddress()+"离开"));
        System.out.println("Client:"+incoming.remoteAddress()+"离开");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("Client:"+incoming.remoteAddress()+"在线");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("Client:"+incoming.remoteAddress()+"掉线");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("Client:"+incoming.remoteAddress()+"异常");

        cause.printStackTrace();
        ctx.close();
    }
}
