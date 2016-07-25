package io.anyway.galaxy.extension;

import io.anyway.galaxy.message.consumer.MessageConsumer;
import io.anyway.galaxy.message.producer.MessageProducer;

import java.util.ServiceLoader;

/**
 * Created by xiong.j on 2016/7/25.
 */
public class ExtensionFactory {
    //TODO 先实现功能，后续根据Dubbo的实现完善
//    ServiceLoader<MessageProducer> messageProducer = ServiceLoader.load(MessageProducer.class);
//
//    ServiceLoader<MessageConsumer> messageConsumer = ServiceLoader.load(MessageConsumer.class);

    public static <S> ServiceLoader<S> getExtension(Class<S> service){
        return ServiceLoader.load(service);
    }

}
