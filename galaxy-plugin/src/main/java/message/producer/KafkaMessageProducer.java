package message.producer;

import io.anyway.galaxy.message.producer.MessageProducer;

/**
 * Created by xiong.j on 2016/7/21.
 */
public class KafkaMessageProducer implements MessageProducer {
    @Override
    public void sendMessage(Object message) {
        System.out.println("KafkaMessageProducer.sendMessage | " + message.toString());
    }
}
