package com.example.user.handler;

import com.alibaba.fastjson.JSONObject;
import com.example.user.entity.Result;
import com.example.user.entity.User;
import com.example.user.service.IUserService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * <p><b>Description:</b>
 *
 * <p><b>Company:</b>
 *
 * @author created by Jesse Hsu at 16:49 on 2020/11/2
 * @version V0.1
 * @classNmae UserServiceHandler
 */
@ChannelHandler.Sharable
@Component
@Slf4j
public class UserServiceHandler extends ChannelInboundHandlerAdapter {

    public final String URI_USER="/user";

    @Autowired
    public IUserService service;

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.channel().flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            long startTime = System.currentTimeMillis();

            FullHttpRequest fullRequest = (FullHttpRequest)msg;

            doHandler(ctx,fullRequest);

        } finally {
            ReferenceCountUtil.release(msg);
        }

    }

    private void doHandler(ChannelHandlerContext ctx,FullHttpRequest fullRequest){

        FullHttpResponse response = null;
        try {
            HttpHeaders requestHeader = fullRequest.headers();
            JSONObject resultJson = new JSONObject();
            JSONObject head = new JSONObject();
            head.put("globalSeqNo",requestHeader.get("globalSeqNo",null));
            resultJson.put("Head",head);
            Result result = doService(fullRequest);
            resultJson.put("Body",result);
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(resultJson.toJSONString().getBytes("UTF-8")));

            response.headers().set("Content-Type", "application/json");
            response.headers().setInt("Content-Length", response.content().readableBytes());
            response.headers().set("globalSeqNo", requestHeader.get("globalSeqNo","null"));
        } catch (Exception e) {
            log.error("user Service 处理出错", e);
            response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
        } finally {
            if (fullRequest != null) {
                if (!HttpUtil.isKeepAlive(fullRequest)) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                    ctx.write(response);
                }

            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    private Result doService(FullHttpRequest fullRequest){
        String uri = fullRequest.uri();
        HttpMethod requestMethod = fullRequest.method();
        Result<User> result =null;
        if(uri.startsWith(URI_USER)){
            //post or put
            if(uri.equals(URI_USER)){
                User user = extractUser(fullRequest);
                if(requestMethod.equals(HttpMethod.PUT)){
                    service.update(user);
                    return Result.buildSuccess("更新成功",String.valueOf(user.getId()));
                }
                if(requestMethod.equals(HttpMethod.POST)){
                    service.insert(user);
                    return Result.buildSuccess("新增成功",String.valueOf(user.getId()));
                }
            }
            //  /user/{id}
            if(requestMethod.equals(HttpMethod.GET)){
                String id = uri.substring(URI_USER.length());
                User user = service.select(Integer.valueOf(id).intValue());
                return Result.buildSuccess(user);
            }
            if(requestMethod.equals(HttpMethod.DELETE)){
                String id = uri.substring(URI_USER.length());
                service.delete(Integer.valueOf(id).intValue());
                return Result.buildSuccess("删除成功",id);
            }

        }
        return null;
    }


    private User extractUser(FullHttpRequest fullRequest){
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(fullRequest);
        List<InterfaceHttpData> httpPostData = decoder.getBodyHttpDatas();
        decoder.offer(fullRequest);
        List<InterfaceHttpData> httpData = decoder.getBodyHttpDatas();
        Map<String,String> requestParams = new HashMap<>();
        httpData.forEach(param->{
            Attribute  attr=(Attribute)param;
            try {
                requestParams.put(attr.getName(),attr.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if(!requestParams.isEmpty()){

            User user = new User();
            user.setId(Integer.valueOf(requestParams.get("id")).intValue());
            user.setFirstName(requestParams.get("firstName"));
            user.setLastName(requestParams.get("lastName"));
            return user;
        }
        return null;
    }
}
