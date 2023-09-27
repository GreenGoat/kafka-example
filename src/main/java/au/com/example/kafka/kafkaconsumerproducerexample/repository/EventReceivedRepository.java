package au.com.example.kafka.kafkaconsumerproducerexample.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventReceivedRepository extends JpaRepository<EventReceivedEntity, Long> {

  EventReceivedEntity findFirstByEventIdAndEventSource(String eventId, String eventSource);

  boolean existsByEventIdAndEventSource(String eventId, String eventSource);
}
