package com.gurkensalat.osm.mosques.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoProducer
{
    private final static Logger LOGGER = LoggerFactory.getLogger(DemoProducer.class);

    @Value("${mq.queue.demo.name}")
    private String queueName;

    @Autowired
    private DemoProducerConfiguration demoProducerConfiguration;

    public void enqueueMessage( /* TaskMessage taskMessage */)
    {
        TaskMessage message = new TaskMessage();
        message.setMessage("Hello from Demo service...");

        LOGGER.info("  sending message <{}>", message);

        demoProducerConfiguration.rabbitTemplate().convertAndSend(queueName, message);
    }
}
