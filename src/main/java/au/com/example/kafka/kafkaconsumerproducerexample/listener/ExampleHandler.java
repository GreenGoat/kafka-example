package au.com.example.kafka.kafkaconsumerproducerexample.listener;

import au.com.example.kafka.kafkaconsumerproducerexample.repository.EventReceivedEntity;
import au.com.example.kafka.kafkaconsumerproducerexample.repository.EventReceivedRepository;
import au.com.example.kafka.kafkaconsumerproducerexample.repository.FailedEventRepository;
import au.com.example.kafka.kafkaconsumerproducerexample.service.ExampleService;
import au.com.example.kafka.kafkaconsumerproducerexample.service.FailedEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExampleHandler {
  private final EventReceivedRepository eventReceivedRepository;
  private final ObjectMapper objectMapper;
  private final ExampleService exampleService;
  private final FailedEventService failedEventService;

  @SneakyThrows
  @Transactional
  public void handleMessage(String eventJson) {
    log.info(" Before Save");
    saveEventReceived(eventJson);
    log.info(" After Save");

    CloudEvent cloudEvent = objectMapper.readValue(eventJson, CloudEvent.class);
    exampleService.doSomething(cloudEvent);
    log.info(" After doSomething");

    deleteFailedEvent(cloudEvent);
    log.info(" After Failed Event cleanup");
  }

  @SneakyThrows
  protected void saveEventReceived(String eventJson) {
    CloudEvent cloudEvent = objectMapper.readValue(eventJson, CloudEvent.class);

    EventReceivedEntity eventReceived =
        EventReceivedEntity.builder()
            .eventId(cloudEvent.id())
            .eventSource(cloudEvent.source())
            .eventTimestamp(cloudEvent.date())
            .createdOn(ZonedDateTime.now())
            .build();

    //Here is the important part
    eventReceivedRepository.saveAndFlush(eventReceived);
  }

  protected void deleteFailedEvent(CloudEvent cloudEvent) {
    try {
      log.info(
          "Checking and deleting failed event with ID: {} that is now processed successfully.",
          cloudEvent.id());
      failedEventService.deleteFailedEvent(cloudEvent.id(), cloudEvent.source());
    } catch (Exception e) {
      log.warn(
          "Error deleting failed event for payment-command id {} and source {}",
          cloudEvent.id(),
          cloudEvent.source());
    }
  }
}
