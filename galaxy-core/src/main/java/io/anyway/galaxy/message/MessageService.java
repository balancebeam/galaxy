package io.anyway.galaxy.message;

import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.message.consumer.MessageConsumer;
import io.anyway.galaxy.message.producer.MessageProducer;

/**
 * Created by xiong.j on 2016/7/25.
 */
public interface MessageService<T> extends MessageConsumer<T>, MessageProducer<T>{
    boolean isProcessed(TransactionMessage message) throws Throwable;
}
