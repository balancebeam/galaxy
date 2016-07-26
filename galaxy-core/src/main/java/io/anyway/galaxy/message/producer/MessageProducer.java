package io.anyway.galaxy.message.producer;

import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.extension.SPI;

/**
 * 消息生产者接口
 *
 * Created by xiong.j on 2016/7/21.
 */
@SPI(value = Constants.KAFKA)
public interface MessageProducer<T> {
    /**
     * 同步发送消息
     *
     * @param message 消息Model
     */
    public void sendMessage(T message) throws Throwable;
}
