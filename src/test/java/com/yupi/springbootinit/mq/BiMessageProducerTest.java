package com.yupi.springbootinit.mq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class BiMessageProducerTest {
    @Resource
    private BiMessageProducer biMessageProducer;
    @Test
    void sendMessage(){
        biMessageProducer.sendMessage("你好！");
    }
}