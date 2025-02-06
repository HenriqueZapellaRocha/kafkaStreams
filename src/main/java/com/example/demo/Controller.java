package com.example.demo;


import com.example.Transaction;
import lombok.Data;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/transaction" )
@Data
public class Controller {


    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    @GetMapping( "/{id}/{value}/{country}" )
    public Void transaction( @PathVariable String id,
                             @PathVariable Integer value,
                             @PathVariable String country ) {


        kafkaTemplate.send( "transactions", id, Transaction.newBuilder()
                                                                .setCountId(id)
                                                                .setCountry(country)
                                                                .setValue(value)
                                                                .build()
                          );
        return null;
    }

}
