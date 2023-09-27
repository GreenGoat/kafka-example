package au.com.example.kafka.kafkaconsumerproducerexample.service;

import au.com.example.kafka.kafkaconsumerproducerexample.repository.FailedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class FailedEventService {

  private final FailedEventRepository failedEventRepository;

  @Autowired
  public FailedEventService(FailedEventRepository failedEventRepository) {
    this.failedEventRepository = failedEventRepository;
  }

  @Transactional
  public void deleteFailedEvent(String eventId, String source) {
    long deleted = failedEventRepository.deleteByEventIdAndEventSource(eventId, source);
    if (deleted > 0) {
      log.info("Deleted failed event id {} and source {}", eventId, source);
    }
  }

  @Transactional
  public void cleanupFailedEvents() {
    int deleted = failedEventRepository.deleteWhereEventReceivedExists();
    if (deleted > 0) {
      log.info(
          "Deleted {} failed event records that have since been reprocessed successfully", deleted);
    }
  }
}
