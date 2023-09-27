package au.com.example.kafka.kafkaconsumerproducerexample.listener;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder(toBuilder = true)
public record CloudEvent(
    String id,
    String source,
    ZonedDateTime date
) {}
