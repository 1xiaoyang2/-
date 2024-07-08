package com.heima.kafka.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class HelloListener {

    @KafkaListener(topics = {"yang-topic"})
    public void hello(String msg){
        System.out.println(msg);
    }
}
