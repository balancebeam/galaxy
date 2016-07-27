package io.anyway.galaxy.message.consumer;

/**
 * 消息消费者接口
 *
 * Created by xiong.j on 2016/7/21.
 */
public interface MessageConsumer<T> {
    /**
     * 处理确认消息,存储到数据库更新为CONFIRMING状态
     * @param message
     * @throws Throwable
     */
    void handleConfirmMessage(T message) throws Throwable;

    /**
     * 处理回滚消息,存储到数据库更新为CANCELLING状态
     * @param message
     * @throws Throwable
     */
    void handleCancelMessage(T message) throws Throwable;
}
