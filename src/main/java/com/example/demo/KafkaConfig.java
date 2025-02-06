package com.example.demo;


import com.example.Transaction;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.streams.StreamsConfig.*;

@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {

        Map<String, Object> props = new HashMap<>();
        props.put( APPLICATION_ID_CONFIG, "servicess" );
        props.put( BOOTSTRAP_SERVERS_CONFIG, "localhost:9094" );
        props.put( "schema.registry.url", "http://localhost:8081" );
        props.put( NUM_STREAM_THREADS_CONFIG, 10 );
        props.put( DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass()) ;
        props.put( DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class );

        props.put( "specific.avro.reader", "true" );

        return new KafkaStreamsConfiguration( props );
    }


    @Bean
    public ProducerFactory<String, Transaction> producerFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");


        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Transaction> kafkaTemplate() {
        return new KafkaTemplate<>( producerFactory() );
    }

}
