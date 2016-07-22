package io.anyway.galaxy.message.consumer;

/**
 * 消息消费者接口
 *
 * Created by xiong.j on 2016/7/21.
 */
public interface MessageConsumer<T> {
    void handleMessage(T message);
}
