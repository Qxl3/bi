package com.yupi.springbootinit.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqInitConfig {
    @Bean
    public DirectExchange exchange(){
        return new DirectExchange("bi_exchange");
    }

    @Bean
    public Queue queue(){
        return new Queue("bi_queue");
    }

    @Bean
    public Binding bindingQueue(Queue queue, DirectExchange directExchange){
        return BindingBuilder.bind(queue).to(directExchange).with("bi_routingKey");
    }

}
