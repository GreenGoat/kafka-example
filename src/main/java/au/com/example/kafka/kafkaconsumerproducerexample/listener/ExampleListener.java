package au.com.example.kafka.kafkaconsumerproducerexample.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExampleListener {

  private final ExampleHandler exampleHandler;

  @KafkaListener(
      topics = {"${spring.kafka.topic.example-topic.name}"},
      groupId = "${spring.kafka.topic.example-topic.group-id}",
      errorHandler = "kafkaErrorHandler"
  )
  public void handleMessage(@Payload String eventJson) {
    log.info("Received Message %s".formatted(eventJson));

    exampleHandler.handleMessage(eventJson);

  }
}
