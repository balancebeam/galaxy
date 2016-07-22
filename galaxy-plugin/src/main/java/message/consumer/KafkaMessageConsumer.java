package message.consumer;

import io.anyway.galaxy.message.consumer.MessageConsumer;

/**
 * Created by xiong.j on 2016/7/21.
 */
public class KafkaMessageConsumer implements MessageConsumer {
    @Override
    public void handleMessage(Object message) {
        System.out.println("KafkaMessageConsumer.handleMessage | " + message.toString());
    }
}
