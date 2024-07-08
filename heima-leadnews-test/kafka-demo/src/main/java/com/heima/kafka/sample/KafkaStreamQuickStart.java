package com.heima.kafka.sample;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaStreamQuickStart {
    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.88.130:9092");
        prop.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        prop.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        prop.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-quickstart");

        //stream 构造器
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        //流式计算
        streamProcessor(streamsBuilder);

        //创建kafkaStream对象
        KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(), prop);

        //开启流式计算
        kafkaStreams.start();
    }

    private static void streamProcessor(StreamsBuilder streamsBuilder) {
        //创建kStream对象，同时指定从那个topic中接收消息
        KStream<String, String> stream = streamsBuilder.stream("itcast-topic-input");

        stream.flatMapValues(new ValueMapper<String, Iterable<String>>() {
            @Override
            public Iterable<String> apply(String s) {
                return Arrays.asList(s.split(" "));
            }
        }).groupBy((key, value) -> value)
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                .count()
                .toStream()
                .map((key, value) -> {
                    System.out.println("key:"+key + ",value:" + value);
                    return new KeyValue<>(key.key().toString(), value.toString());
                })
                .to("itcast-topic-out");
    }
}
