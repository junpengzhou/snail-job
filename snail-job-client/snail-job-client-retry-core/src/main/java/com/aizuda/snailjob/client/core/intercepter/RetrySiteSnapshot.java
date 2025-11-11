package com.aizuda.snailjob.client.core.intercepter;

import com.aizuda.snailjob.client.core.RetryArgSerializer;
import com.aizuda.snailjob.client.core.RetrySiteSnapshotContext;
import com.aizuda.snailjob.client.core.annotation.Propagation;
import com.aizuda.snailjob.client.core.annotation.Retryable;
import com.aizuda.snailjob.client.core.exception.SnailRetryClientException;
import com.aizuda.snailjob.client.core.loader.SnailRetrySpiLoader;
import com.aizuda.snailjob.common.core.constant.SystemConstants;
import com.aizuda.snailjob.common.core.model.SnailJobHeaders;
import lombok.Getter;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 重试现场记录器
 *
 * @author: opensnail
 * @date : 2022-03-03 13:42
 */
public class RetrySiteSnapshot {

    private static final String RETRY_STAGE_KEY = "RETRY_STAGE";
    private static final String RETRY_CLASS_METHOD_ENTRANCE_KEY = "RETRY_CLASS_METHOD_ENTRANCE";
    private static final String RETRY_STATUS_KEY = "RETRY_STATUS";

    /**
     * 重试阶段，1-内存重试阶段，2-服务端重试阶段
     */
    private static final RetrySiteSnapshotContext<Integer> RETRY_STAGE = SnailRetrySpiLoader.loadRetrySiteSnapshotContext();

    /**
     * 标记重试方法入口
     */
    private static final RetrySiteSnapshotContext<Deque<MethodEntranceMeta>> RETRY_CLASS_METHOD_ENTRANCE = SnailRetrySpiLoader.loadRetrySiteSnapshotContext();

    /**
     * 重试状态
     */
    private static final RetrySiteSnapshotContext<Integer> RETRY_STATUS = SnailRetrySpiLoader.loadRetrySiteSnapshotContext();

    /**
     * 重试请求头
     */
    private static final RetrySiteSnapshotContext<SnailJobHeaders> RETRY_HEADER = SnailRetrySpiLoader.loadRetrySiteSnapshotContext();

    /**
     * 状态码
     */
    private static final RetrySiteSnapshotContext<String> RETRY_STATUS_CODE = SnailRetrySpiLoader.loadRetrySiteSnapshotContext();

    /**
     * 挂起重试的内存状态
     */
    private static final RetrySiteSnapshotContext<Map<String, Object>> SUSPEND = SnailRetrySpiLoader.loadRetrySiteSnapshotContext();

    /**
     * 进入方法入口时间标记
     */
    private static final RetrySiteSnapshotContext<Long> ENTRY_METHOD_TIME = SnailRetrySpiLoader.loadRetrySiteSnapshotContext();

    public static void suspend() {
        SUSPEND.set(new HashMap<>() {{
            put(RETRY_STAGE_KEY, RETRY_STAGE.get());
            put(RETRY_STATUS_KEY, RETRY_STATUS.get());
            put(RETRY_CLASS_METHOD_ENTRANCE_KEY, deepCopyRetryClassMethodEntrance());
        }});
    }

    /**
     * 深拷贝，防止数据被错误弹出
     *
     * @return 新的入口栈信息
     */
    private static Deque<MethodEntranceMeta> deepCopyRetryClassMethodEntrance() {
        Deque<MethodEntranceMeta> methodEntranceMetas = RETRY_CLASS_METHOD_ENTRANCE.get();
        Deque<MethodEntranceMeta> newMethodEntranceMetas = new LinkedBlockingDeque<>();
        for (MethodEntranceMeta methodEntranceMeta : methodEntranceMetas) {
            newMethodEntranceMetas.push(methodEntranceMeta);
        }
        return newMethodEntranceMetas;
    }

    public static void restore() {
        Optional.ofNullable(SUSPEND.get()).ifPresent(map -> {
            RETRY_STAGE.set((Integer) map.get(RETRY_STAGE_KEY));
            RETRY_STATUS.set((Integer) map.get(RETRY_STATUS_KEY));
            RETRY_CLASS_METHOD_ENTRANCE.set((Deque<MethodEntranceMeta>) map.get(RETRY_CLASS_METHOD_ENTRANCE_KEY));
            SUSPEND.remove();
        });
    }

    public static void removeSuspend() {
        SUSPEND.remove();
    }

    public static Integer getStage() {
        return RETRY_STAGE.get();
    }

    public static void setStage(int stage) {
        RETRY_STAGE.set(stage);
    }

    public static MethodEntranceMeta getMethodEntranceMeta() {
        Deque<MethodEntranceMeta> stack = RETRY_CLASS_METHOD_ENTRANCE.get();
        if (Objects.isNull(stack) || Objects.isNull(stack.peek())) {
            return null;
        }

        return stack.peek();
    }

    public static String getMethodEntrance() {
        Deque<MethodEntranceMeta> stack = RETRY_CLASS_METHOD_ENTRANCE.get();
        if (Objects.isNull(stack) || Objects.isNull(stack.peek())) {
            return null;
        }

        return stack.peek().getMethodEntrance();
    }

    public static boolean existedMethodEntrance() {
        Deque<MethodEntranceMeta> stack = RETRY_CLASS_METHOD_ENTRANCE.get();
        if (Objects.isNull(stack)) {
            return Boolean.FALSE;
        }

        MethodEntranceMeta meta = stack.peek();
        if (Objects.isNull(meta)) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    public static void setMethodEntranceMeta(MethodEntranceMeta methodEntranceMeta) {
        Deque<MethodEntranceMeta> stack = RETRY_CLASS_METHOD_ENTRANCE.get();
        if (Objects.isNull(RETRY_CLASS_METHOD_ENTRANCE.get())) {
            stack = new LinkedBlockingDeque<>();
            RETRY_CLASS_METHOD_ENTRANCE.set(stack);
        }

        if (!isRunning() && !isRetryFlow()) {
            stack.push(methodEntranceMeta);
        }
    }

    public static void removeMethodEntranceMeta() {
        Deque<MethodEntranceMeta> stack = RETRY_CLASS_METHOD_ENTRANCE.get();
        if (Objects.isNull(stack)) {
            return;
        }

        if (stack.isEmpty()) {
            RETRY_CLASS_METHOD_ENTRANCE.remove();
            return;
        }

        if (!isRunning() && !isRetryFlow()) {
            stack.pop();
        }

    }

    public static boolean isMethodEntrance(MethodEntranceMeta methodEntrance) {
        Deque<MethodEntranceMeta> stack = RETRY_CLASS_METHOD_ENTRANCE.get();
        if (Objects.isNull(stack) || Objects.isNull(stack.peek()) || Objects.isNull(methodEntrance)) {
            return Boolean.FALSE;
        }

        MethodEntranceMeta peek = stack.peek();
        return methodEntrance.getMethodEntrance().equals(peek.getMethodEntrance());
    }

    public static Integer getStatus() {
        return Optional.ofNullable(RETRY_STATUS.get()).orElse(EnumStatus.COMPLETE.status);

    }

    public static void setStatus(Integer status) {
        RETRY_STATUS.set(status);
    }

    public static boolean isRunning() {
        return EnumStatus.RUNNING.status == getStatus();
    }

    public static SnailJobHeaders getRetryHeader() {
        return RETRY_HEADER.get();
    }

    public static void setRetryHeader(SnailJobHeaders headers) {
        RETRY_HEADER.set(headers);
    }

    /**
     * 是否是重试流量
     */
    public static boolean isRetryFlow() {
        SnailJobHeaders retryHeader = getRetryHeader();
        if (Objects.nonNull(retryHeader)) {
            return retryHeader.isRetry();
        }

        return false;
    }

    public static String getRetryStatusCode() {
        return RETRY_STATUS_CODE.get();
    }

    public static void setRetryStatusCode(String statusCode) {
        RETRY_STATUS_CODE.set(statusCode);
    }

    public static boolean isRetryForStatusCode() {
        return Objects.nonNull(getRetryStatusCode()) && getRetryStatusCode()
                .equals(SystemConstants.SNAIL_JOB_STATUS_CODE);
    }

    public static Long getEntryMethodTime() {
        return ENTRY_METHOD_TIME.get();
    }

    public static void setEntryMethodTime(long entryMethodTime) {
        ENTRY_METHOD_TIME.set(entryMethodTime);
    }

    public static void removeEntryMethodTime() {
        ENTRY_METHOD_TIME.remove();
    }

    public static void removeRetryHeader() {
        RETRY_HEADER.remove();
    }

    public static void removeRetryStatusCode() {
        RETRY_STATUS_CODE.remove();
    }

    public static void removeStage() {
        RETRY_STAGE.remove();
    }

    public static void removeStatus() {
        RETRY_STATUS.remove();
    }

    public static void removeAll() {

        removeStatus();
        removeStage();
        removeEntryMethodTime();
        removeRetryHeader();
        removeRetryStatusCode();
        removeMethodEntranceMeta();
    }

    /**
     * 重试阶段
     */
    @Getter
    public enum EnumStage {

        /**
         * 本地重试阶段
         */
        LOCAL(1),

        /**
         * 远程重试阶段
         */
        REMOTE(2),

        /**
         * 手动提交数据
         */
        MANUAL_REPORT(3),
        ;

        private final int stage;

        EnumStage(int stage) {
            this.stage = stage;
        }

        public static EnumStage valueOfStage(int stage) {
            for (final EnumStage value : EnumStage.values()) {
                if (value.getStage() == stage) {
                    return value;
                }
            }

            throw new SnailRetryClientException("unsupported stage");
        }

    }

    /**
     * 重试状态
     */
    @Getter
    public enum EnumStatus {

        /**
         * 重试中
         */
        RUNNING(1),

        /**
         * 重试完成
         */
        COMPLETE(2),
        ;

        private final int status;

        EnumStatus(int status) {
            this.status = status;
        }

    }

    public interface MethodEntranceMeta {

        MethodInvocation getMethodInvocation();

        String getMethodEntrance();

        Object[] getMethodParams();

        Retryable getRetryable();

        String getExecutorClassName();

        boolean isRequiresNew();

        boolean isThrowException();
    }

    public static class DefaultMethodEntranceMeta implements MethodEntranceMeta {

        private final MethodInvocation methodInvocation;

        public DefaultMethodEntranceMeta(MethodInvocation methodInvocation) {
            this.methodInvocation = methodInvocation;
        }

        @Override
        public MethodInvocation getMethodInvocation() {
            return methodInvocation;
        }

        @Override
        public String getMethodEntrance() {
            return "";
        }

        @Override
        public Object[] getMethodParams() {
            return methodInvocation.getArguments();
        }

        @Override
        public Retryable getRetryable() {
            return null;
        }

        @Override
        public String getExecutorClassName() {
            return "";
        }

        @Override
        public boolean isRequiresNew() {
            return false;
        }

        @Override
        public boolean isThrowException() {
            return true;
        }
    }

    @Getter
    public static abstract class AbstractMethodEntranceMeta implements MethodEntranceMeta {

        private final MethodInvocation methodInvocation;

        private final String methodEntrance;

        private final Retryable retryable;

        private final String executorClassName;

        public AbstractMethodEntranceMeta(MethodInvocation methodInvocation) {
            this.methodInvocation = methodInvocation;
            this.retryable = getAnnotationParameter(methodInvocation.getMethod());
            this.executorClassName = methodInvocation.getThis().getClass().getName();
            this.methodEntrance = retryable.scene().concat("_").concat(executorClassName);
            ;
        }

        @Override
        public boolean isRequiresNew() {
            return Propagation.REQUIRES_NEW.equals(retryable.propagation());
        }

        @Override
        public boolean isThrowException() {
            return retryable.isThrowException();
        }
    }

    @Getter
    public static class ParamsReferenceMethodEntranceMeta extends AbstractMethodEntranceMeta {

        private final Object[] methodParams;

        public ParamsReferenceMethodEntranceMeta(MethodInvocation methodInvocation) {
            super(methodInvocation);
            this.methodParams = methodInvocation.getArguments();
        }
    }

    @Getter
    public static class ParamsCopyMethodEntranceMeta extends AbstractMethodEntranceMeta {

        private final String methodParamsStr;

        public ParamsCopyMethodEntranceMeta(MethodInvocation methodInvocation) {

            super(methodInvocation);

            RetryArgSerializer retryArgSerializer = SnailRetrySpiLoader.loadRetryArgSerializer();
            this.methodParamsStr = retryArgSerializer.serialize(methodInvocation.getArguments());
        }

        @Override
        public Object[] getMethodParams() {
            RetryArgSerializer retryArgSerializer = SnailRetrySpiLoader.loadRetryArgSerializer();
            return (Object[]) retryArgSerializer.deSerialize(methodParamsStr, getMethodInvocation().getClass(), getMethodInvocation().getMethod());
        }
    }


    private static Retryable getAnnotationParameter(Method method) {

        Retryable retryable = null;
        if (method.isAnnotationPresent(Retryable.class)) {
            //获取当前类的方法上标注的注解对象
            retryable = method.getAnnotation(Retryable.class);
        }

        if (retryable == null) {
            // 返回当前类或父类或接口方法上标注的注解对象
            retryable = AnnotatedElementUtils.findMergedAnnotation(method, Retryable.class);
        }

        return retryable;
    }
}
