package com.aizuda.snailjob.client.core.cache;

import com.aizuda.snailjob.client.core.RetryArgSerializer;
import com.aizuda.snailjob.client.core.loader.SnailRetrySpiLoader;
import com.aizuda.snailjob.client.core.serializer.ForySerializer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: ylchai
 */
public class RetryArgSerializerCache {
    private final static ConcurrentHashMap<String, RetryArgSerializer> RETRY_ARG_SERIALIZER_MAP = new ConcurrentHashMap<>();

    private final static RetryArgSerializer DEFAULT_SERIALIZER = SnailRetrySpiLoader.loadRetryArgSerializer();

    private RetryArgSerializerCache() {
    }

    /**
     * 加载参数序列化SPI类
     * 若配置多个则只加载第一个
     *
     * @return {@link ForySerializer} 默认序列化类为ForySerializer
     */
    public static RetryArgSerializer retryArgSerializer() {
        return DEFAULT_SERIALIZER;
    }

    /**
     * 加载指定名称的参数序列化SPI类
     *
     * @return {@link ForySerializer} 默认序列化类为ForySerializer
     */
    public static RetryArgSerializer retryArgSerializer(String name) {
        return RETRY_ARG_SERIALIZER_MAP.computeIfAbsent(name, SnailRetrySpiLoader::loadRetryArgSerializer);
    }
}
