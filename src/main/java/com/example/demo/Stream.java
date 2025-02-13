package com.example.demo;

import com.example.AccountAlert;
import com.example.Transaction;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@Data
public class Stream {

    private final SpecificAvroSerde<Transaction> transactionSerde;
    private final SpecificAvroSerde<AccountAlert> alertSerde;


    @Bean
    public KStream<String, Transaction> kafkaStream( StreamsBuilder streamsBuilder ) {

        KStream<String, Transaction> productStream = streamsBuilder.stream(
                "transactions", Consumed.with( Serdes.String(), transactionSerde ) );

        var validationStream = productStream
                .peek( ( key,value) -> log.info( "transaction information: {}", value.toString() ) )
                .split( Named.as( "validating-" ))
                .branch( (key, value) -> value.getValue() < 0, Branched.as("invalid" ) )
                .defaultBranch( Branched.as( "valid" ) );

        validationStream.get( "validating-invalid" )
                            .to("invalid", Produced.with( Serdes.String(), transactionSerde ));

        validationStream.get( "validating-valid" )
                            .to("valid", Produced.with( Serdes.String(), transactionSerde ));

        validationStream.get( "validating-valid" )
                .filter( ( key, transaction ) -> transaction.getValue() >= 1000 )
                .groupByKey()
                .windowedBy( TimeWindows.ofSizeWithNoGrace( Duration.ofSeconds( 3 ) ) )
                .count()
                .suppress( Suppressed.untilWindowCloses( Suppressed.BufferConfig.unbounded() ) )
                .toStream()
                .map( (key, value) -> KeyValue.pair( key.key(), value ) )
                .filter( (key, value) -> value >= 3 )
                .peek( ( (key, value) -> log.info( "transaction rejected. Count id: {}", key ) ) )
                .mapValues( (readOnlyKey, value) -> AccountAlert.newBuilder()
                                                                .setCountId( readOnlyKey )
                                                                .setProblem( "several suspicious transactions " +
                                                                             "in a short period of time" )
                                                                .build()
                )
                .to( "high-transaction-times", Produced.with( Serdes.String(), alertSerde ) );

        validationStream.get( "validating-valid" )
                .groupByKey()
                .windowedBy( TimeWindows.ofSizeAndGrace( Duration.ofSeconds( 5 ), Duration.ofSeconds( 1 ) ) )
                .aggregate(
                        () -> "UNKNOWN",
                        (key, value, lastLocation) -> {
                            String currentLocation = value.getCountry();
                            if ( ( !lastLocation.equals( "UNKNOWN" ) && !lastLocation.equals( currentLocation ) )) {
                                return currentLocation + " (SUSPICIOUS)";
                            }
                            return currentLocation;
                        },
                        Materialized.with( Serdes.String(), Serdes.String() )
                )
                .toStream()
                .filter( (key, value) -> value.contains( "SUSPICIOUS" ) )
                .map( (key, value) -> KeyValue.pair( key.key(), "ALERT: Location changed - " + value ) )
                .peek( (key, value) -> log.info( "country mutation. Count id: {}", key ) )
                .mapValues( (readOnlyKey, value) -> AccountAlert.newBuilder()
                                                                .setCountId( readOnlyKey )
                                                                .setProblem( "different country transactions " +
                                                                             "in a short time" )
                                                                .build()
                )
                .to("different-country-mutation", Produced.with( Serdes.String(), alertSerde ) );
        return productStream;
    }

}
