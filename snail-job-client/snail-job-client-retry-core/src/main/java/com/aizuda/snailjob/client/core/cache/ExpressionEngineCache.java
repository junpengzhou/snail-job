package com.aizuda.snailjob.client.core.cache;

import com.aizuda.snailjob.client.core.loader.SnailRetrySpiLoader;
import com.aizuda.snailjob.common.core.expression.ExpressionEngine;

/**
 * @author: ylchai
 */
public class ExpressionEngineCache {

    private final static ExpressionEngine DEFAULT_EXPRESSION_ENGINE = SnailRetrySpiLoader.loadExpressionEngine();

    private ExpressionEngineCache() {
    }

    /**
     * 加载表达式引擎SPI类
     * 若配置多个则只加载第一个
     *
     * @return {@link ExpressionEngine} 默认表达式引擎
     */
    public static ExpressionEngine expressionEngine() {
        return DEFAULT_EXPRESSION_ENGINE;
    }
}

