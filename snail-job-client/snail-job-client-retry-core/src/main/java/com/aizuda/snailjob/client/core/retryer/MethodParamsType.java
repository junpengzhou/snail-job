package com.aizuda.snailjob.client.core.retryer;

/**
 * @author: xiaochaihu
 * @date : 2025-11-11 21:12
 */
public enum MethodParamsType {

    /**
     * 重试方法执行时参数引用保持不变，如果方法内部对参数进行修改，重试时的数据是修改后的数据
     */
    REFERENCE,

    /**
     * 对方法参数进行深拷贝，重试时的数据是拷贝后的数据，方法外部的引用会丢失
     */
    DEEP_COPY,
}
