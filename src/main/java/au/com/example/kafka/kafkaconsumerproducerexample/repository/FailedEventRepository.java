package au.com.example.kafka.kafkaconsumerproducerexample.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEventEntity, Long> {

  Optional<FailedEventEntity> findFirstByEventIdAndEventSource(String eventId, String eventSource);

  Optional<FailedEventEntity>
      findFirstByRetryCountLessThanAndRequeueActionEqualsOrderByFailureTimestamp(
          Integer maxRetryCount, String requeueAction);

  long deleteByEventIdAndEventSource(String eventId, String eventSource);

  @Modifying
  @Query(
      """
                    delete from FailedEventEntity f where exists
                      (select 1 from EventReceivedEntity e
                        where f.eventId = e.eventId
                        and f.eventSource = e.eventSource)
                    """)
  int deleteWhereEventReceivedExists();

  @Query(
      value =
          "select min(fe.failureTimestamp) from FailedEventEntity fe where fe.requeueAction = :requeueAction")
  ZonedDateTime findOldestTimestampByAction(@Param("requeueAction") String requeueAction);

  @Query(
      nativeQuery = true,
      value =
          """
                            select *
                                from failed_event
                                where retry_count >= :maxRetries and requeue_action = :requeueAction
                                and failure_timestamp < :failedBeforeTimestamp
                                and event_timestamp >= :eventAfterTimestamp
                                limit :maxBatchSize
                            """)
  List<FailedEventEntity> findRetryable(
      @Param("maxRetries") Integer maxRetries,
      @Param("requeueAction") String requeueAction,
      @Param("failedBeforeTimestamp") ZonedDateTime failedBeforeTimestamp,
      @Param("eventAfterTimestamp") ZonedDateTime eventAfterTimestamp,
      @Param("maxBatchSize") Integer maxBatchSize);
}
