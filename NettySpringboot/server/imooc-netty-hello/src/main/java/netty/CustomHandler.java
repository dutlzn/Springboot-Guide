package netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * @Description: 创建自定义助手类
 */
// SimpleChannelInboundHandler 对于请求来讲，其实相当于 入站 入境
public class CustomHandler extends SimpleChannelInboundHandler<HttpObject> {
    /**
     * channael的生命周期
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("注册");
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("移除");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("活跃");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("不活跃");
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("读取完毕");
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("用户事件触发");
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        System.out.println("可写更改");
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("捕获到异常");
        super.exceptionCaught(ctx, cause);
    }

    @Override // 从缓冲区读取数据
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
            throws Exception {
        // 获取channel
        Channel channel = ctx.channel();

        if (msg instanceof  HttpRequest){

            // 显示客户端的远程地址
            System.out.println(channel.remoteAddress());

            // 定义发送的数据消息
            ByteBuf content = Unpooled.copiedBuffer("Hello netty~", CharsetUtil.UTF_8);//缓冲区

            // 构建一个Http response
            FullHttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                            HttpResponseStatus.OK,
                            content);

            // 为响应增加数据类型和长度
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

            //把响应刷到客户端
            ctx.writeAndFlush(response);
        }


    }
}
