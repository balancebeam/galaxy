package io.anyway.galaxy.message.producer;

import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.exception.DistributedTransactionException;
import io.anyway.galaxy.message.TransactionMessage;
import io.anyway.galaxy.repository.TransactionRepository;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiong.j on 2016/7/21.
 */
public class KafkaMessageProducer implements MessageProducer<TransactionMessage>,InitializingBean,DisposableBean {

    private final static Log logger= LogFactory.getLog(KafkaMessageProducer.class);

    @Value("${kafka.servers}")
    private String servers;

    @Value("${kafka.producer.timeout}")
    private int timeout= 30;

    private Producer<String, TransactionMessage> producer;

    @Override
    public void sendMessage(TransactionMessage message) {
        Future<RecordMetadata> future=  producer.send(new ProducerRecord<String, TransactionMessage>("galaxy-tx-message",message));
        try {
            RecordMetadata metadata= future.get(timeout, TimeUnit.SECONDS);
            if(logger.isInfoEnabled()){
                logger.info("send message success: "+metadata);
            }
        } catch (Exception e) {
            throw new DistributedTransactionException("send message error: "+message,e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "io.anyway.galaxy.message.serialization.TransactionMessageSerializer");
        producer = new KafkaProducer<String,TransactionMessage>(props);
        if(logger.isInfoEnabled()){
            logger.info("crete kafka producer: "+producer);
        }
    }

    @Override
    public void destroy() throws Exception {
        if(logger.isInfoEnabled()){
            logger.info("destroy kafka producer: "+producer);
        }
        producer.close();
    }
}
