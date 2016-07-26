package io.anyway.galaxy.message.consumer;

import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.extension.SPI;

/**
 * 消息消费者接口
 *
 * Created by xiong.j on 2016/7/21.
 */
@SPI(value = Constants.KAFKA)
public interface MessageConsumer<T> {
    void handleMessage(T message) throws Throwable ;
}
