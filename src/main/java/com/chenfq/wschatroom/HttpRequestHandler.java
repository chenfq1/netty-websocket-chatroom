package com.chenfq.wschatroom;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

//1.用于处理FullHttpRequest信息
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String wsUri;

    private static final File INDEX;

    static {
        URL location =
                HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try{
            String path = location.toURI() + "WebsocketChatClient.html";
            path = !path.contains("file:")?path : path.substring(5);
            INDEX = new File(path);
        }catch (URISyntaxException e){
            throw new IllegalStateException("unable to locate WebsocketChatClient.html");
        }
    }

    public HttpRequestHandler(String wsUri){
        this.wsUri = wsUri;
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {

        if(wsUri.equalsIgnoreCase(fullHttpRequest.getUri())){
            channelHandlerContext.fireChannelRead(fullHttpRequest.retain());//2.如果请求的是websocket升级，保留请求传递给下个handler
        }else {

            if(HttpUtil.is100ContinueExpected(fullHttpRequest)){ //3.处理符合http1.1的"100 continue"请求
                send100Continue(channelHandlerContext);
            }

            //4.读取默认的WebsocketChatClient页面
            RandomAccessFile file = new RandomAccessFile(INDEX,"r");

            HttpResponse response = new DefaultHttpResponse(fullHttpRequest.getProtocolVersion(), HttpResponseStatus.OK);
            response.headers().set(HttpHeaders.Names.CONTENT_TYPE,"text/html;charset=UTF-8");
            boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest);

            if(keepAlive){   //5.判断keepalive是否在请求头里面

                response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,file.length());
                response.headers().set(HttpHeaders.Names.CONNECTION,HttpHeaders.Values.KEEP_ALIVE);
            }
            //6.写httpresponse到客户端
            channelHandlerContext.write(response);

            if(channelHandlerContext.pipeline().get(SslHandler.class) == null){
                //7. 写index到客户端，判断SslHandler是否在channelPipeline来决定使用defaultFileRegion还是chunkedNioFile
                channelHandlerContext.write(new DefaultFileRegion(file.getChannel(),0,file.length()));
            }else {
                channelHandlerContext.write(new ChunkedNioFile(file.getChannel()));
            }

            //8.写并刷新lastHttpContent到客户端，标记响应完成
            ChannelFuture future = channelHandlerContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            if (!keepAlive){
                //9.如果keepalive没有要求，写完成关闭channel
                future.addListener(ChannelFutureListener.CLOSE);
            }

            file.close();

        }
    }

    private static void send100Continue(ChannelHandlerContext ctx){
        HttpResponse response =  new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("Client: " + incoming.remoteAddress() + " 异常");

        //出现异常关闭连接
        ctx.close();
        cause.printStackTrace();

    }
}
