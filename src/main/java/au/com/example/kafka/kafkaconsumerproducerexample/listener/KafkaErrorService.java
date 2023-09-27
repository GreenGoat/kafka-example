package au.com.example.kafka.kafkaconsumerproducerexample.listener;

import au.com.example.kafka.kafkaconsumerproducerexample.repository.EventReceivedRepository;
import au.com.example.kafka.kafkaconsumerproducerexample.repository.FailedEventEntity;
import au.com.example.kafka.kafkaconsumerproducerexample.repository.FailedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaErrorService {
  private final ObjectMapper objectMapper;
  private final EventReceivedRepository eventReceivedRepository;
  private final FailedEventRepository failedEventRepository;
  private final KafkaTemplate kafkaTemplate;

  @Value("${spring.kafka.deadletter.suffix:-dlq-ms-payment-approval-api}")
  private String dlqSuffix;

  @SneakyThrows
  @Transactional
  public void handleError(Message<?> message, ListenerExecutionFailedException exception) {

    String originalTopicName = (String) message.getHeaders().get("kafka_receivedTopic");
    String payload = message.getPayload().toString();

    log.error("Failed to process Message %s".formatted(payload), exception.getCause());

    var cloudEvent = readPayload(payload);

    FailedEventEntity failedEvent = toFailedEvent(cloudEvent, payload, exception, originalTopicName);

    if (isDuplicateEventFailure(exception, failedEvent)) {
      log.info("Duplicate event detected from topic: {}. Ignoring it. ID={}, source={}, subject={}",
          originalTopicName,
          failedEvent.getEventId(),
          failedEvent.getEventSource(),
          failedEvent.getSubject());
    }


    try {
      createOrUpdateFailedEvent(failedEvent);
      return;
    } catch (Exception e) {
      log.error(
          "Unable to save failed message into the database - sending to dead-letter queue instead. Id={}, source={}, subject={}",
          failedEvent.getId(),
          failedEvent.getEventSource(),
          failedEvent.getSubject(),
          e);
    }

    sendToDeadletterQueue(originalTopicName, message);
  }

  private Optional<CloudEvent> readPayload(String payload) {
    try {
      return Optional.of(objectMapper.readValue(payload, CloudEvent.class));
    } catch (JsonProcessingException jsonProcessingException) {
      log.error("Error reading cloud event details from payload.", jsonProcessingException);
    }
    return Optional.empty();
  }

  private void createOrUpdateFailedEvent(FailedEventEntity failedEvent) {
    Optional<FailedEventEntity> previousVersionInfo =
        failedEventRepository.findFirstByEventIdAndEventSource(
            failedEvent.getEventId(), failedEvent.getEventSource());
    if (failedEvent.getEventId() != null && previousVersionInfo.isPresent()) {
      FailedEventEntity previousVersion = previousVersionInfo.get();
      failedEventRepository.saveAndFlush(
          previousVersion.toBuilder()
              .requeueAction(failedEvent.getRequeueAction())
              .failureTimestamp(ZonedDateTime.now())
              .message(failedEvent.getMessage())
              .failureMessage(failedEvent.getFailureMessage())
              .build());
    } else {
      failedEventRepository.saveAndFlush(failedEvent);
    }
  }

  private void sendToDeadletterQueue(String originalTopicName, Message<?> message) {
    String deadLetterQueueName = originalTopicName + dlqSuffix;
    try {
      kafkaTemplate.send(deadLetterQueueName, message.getPayload().toString());
    } catch (Exception exception) {
      log.error(
          "Failed to send message to dead-letter queue: topic={} because: {}",
          deadLetterQueueName,
          exception.getMessage());
      throw exception;
    }
  }

  private FailedEventEntity toFailedEvent(
      Optional<CloudEvent> cloudEvent,
      String payload,
      ListenerExecutionFailedException exception,
      String originalTopicName) {

    FailedEventEntity.FailedEventEntityBuilder failedEventEntityBuilder = FailedEventEntity.builder();

    if(cloudEvent.isPresent() && cloudEvent.get().id() != null) {
      CloudEvent cloudEvent1 = cloudEvent.get();
      failedEventEntityBuilder
          .eventId(cloudEvent1.id())
          .eventSource(cloudEvent1.source())
          .eventTimestamp(cloudEvent1.date())
          .requeueAction(FailedEventEntity.ACTION_FAILED);
    } else {
      failedEventEntityBuilder
          .requeueAction(FailedEventEntity.ACTION_IGNORED);
    }

    return failedEventEntityBuilder
        .message(payload)
        .requeueTopic(originalTopicName)
        .failureMessage(exception.getCause().getMessage()).build();
  }

  private boolean isDuplicateEventFailure(
      ListenerExecutionFailedException exception, FailedEventEntity failedEvent) {

    if (ExceptionUtils.getThrowableList(exception).stream()
            .anyMatch(DataIntegrityViolationException.class::isInstance)
        && failedEvent.getEventId() != null
        && failedEvent.getEventSource() != null) {
      return eventReceivedRepository.existsByEventIdAndEventSource(
          failedEvent.getEventId(), failedEvent.getEventSource());
    }

    return false;
  }

}
