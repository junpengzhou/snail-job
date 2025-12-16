package com.aizuda.snailjob.client.common.rpc.client.grpc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.aizuda.snailjob.client.common.cache.GroupVersionCache;
import com.aizuda.snailjob.client.common.config.SnailJobProperties;
import com.aizuda.snailjob.client.common.exception.SnailJobRemoteException;
import com.aizuda.snailjob.client.common.rpc.client.common.ClientInfo;
import com.aizuda.snailjob.common.core.constant.SystemConstants;
import com.aizuda.snailjob.common.core.context.SnailSpringContext;
import com.aizuda.snailjob.common.core.enums.ExecutorTypeEnum;
import com.aizuda.snailjob.common.core.enums.HeadersEnum;
import com.aizuda.snailjob.common.core.grpc.auto.GrpcResult;
import com.aizuda.snailjob.common.core.grpc.auto.SnailJobGrpcRequest;
import com.aizuda.snailjob.common.core.grpc.auto.Metadata;
import com.aizuda.snailjob.common.core.util.SnailJobVersion;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author: opensnail
 * @date : 2024-08-22
 */
public final class GrpcChannel {

    private static ManagedChannel channel;
    public static void setChannel(ManagedChannel channel) {
        GrpcChannel.channel = channel;
    }


    public static ListenableFuture<GrpcResult> sendOfUnary(String path, String body, long reqId, Map<String, String> map) {
        if (channel == null) {
            return null;
        }

        SnailJobProperties snailJobProperties = SnailSpringContext.getBean(SnailJobProperties.class);

        // server配置不能为空
        SnailJobProperties.ServerConfig serverConfig = snailJobProperties.getServer();
        if (Objects.isNull(serverConfig)) {
            SnailJobLog.LOCAL.error("snail job server config is null");
            return null;
        }

        Assert.notBlank(snailJobProperties.getGroup(),
            () -> new SnailJobRemoteException("The group is null, please check if your configuration is correct."));

        Map<String, String> headersMap = new HashMap<>();

        headersMap.put(HeadersEnum.HOST_ID.getKey(), ClientInfo.HOST_ID);
        headersMap.put(HeadersEnum.HOST_IP.getKey(), ClientInfo.getClientHost());
        headersMap.put(HeadersEnum.GROUP_NAME.getKey(), snailJobProperties.getGroup());
        headersMap.put(HeadersEnum.HOST_PORT.getKey(), String.valueOf(ClientInfo.getClientPort()));
        headersMap.put(HeadersEnum.VERSION.getKey(), String.valueOf(GroupVersionCache.getVersion()));
        headersMap.put(HeadersEnum.HOST.getKey(), serverConfig.getHost());
        headersMap.put(HeadersEnum.NAMESPACE.getKey(), Optional.ofNullable(snailJobProperties.getNamespace()).orElse(
            SystemConstants.DEFAULT_NAMESPACE));
        headersMap.put(HeadersEnum.TOKEN.getKey(), Optional.ofNullable(snailJobProperties.getToken()).orElse(
            SystemConstants.DEFAULT_TOKEN));
        headersMap.put(HeadersEnum.SYSTEM_VERSION.getKey(), Optional.ofNullable(SnailJobVersion.getVersion()).orElse(
                SystemConstants.DEFAULT_CLIENT_VERSION));
        headersMap.put(HeadersEnum.EXECUTOR_TYPE.getKey(), String.valueOf(ExecutorTypeEnum.JAVA.getType()));
        if (CollUtil.isNotEmpty(map)) {
            headersMap.putAll(map);
        }

        Metadata metadata = Metadata
            .newBuilder()
            .setUri(path)
            .putAllHeaders(headersMap)
            .build();
        SnailJobGrpcRequest snailJobRequest = SnailJobGrpcRequest
            .newBuilder()
            .setMetadata(metadata)
            .setReqId(reqId)
            .setBody(body)
            .build();

        MethodDescriptor<SnailJobGrpcRequest, GrpcResult> methodDescriptor =
            MethodDescriptor.<SnailJobGrpcRequest, GrpcResult>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName("UnaryRequest", "unaryRequest"))
                .setRequestMarshaller(ProtoUtils.marshaller(SnailJobGrpcRequest.getDefaultInstance()))
                .setResponseMarshaller(ProtoUtils.marshaller(GrpcResult.getDefaultInstance()))
                .build();

        // 创建动态代理调用方法
        return io.grpc.stub.ClientCalls.futureUnaryCall(
            channel.newCall(methodDescriptor, io.grpc.CallOptions.DEFAULT),
            snailJobRequest);
    }

}
