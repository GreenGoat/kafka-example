package au.com.example.kafka.kafkaconsumerproducerexample.repository;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity
@Table(name = "FAILED_EVENT")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class FailedEventEntity {

  public static final String ACTION_FAILED = "FAILED";
  public static final String ACTION_REQUEUE = "REQUEUE";
  public static final String ACTION_IGNORED = "IGNORED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String eventId;

  private String eventSource;

  private String subject;

  private ZonedDateTime eventTimestamp;

  private String message;

  @Builder.Default private String requeueAction = ACTION_FAILED;

  @Builder.Default private Integer retryCount = 0;

  @Builder.Default private ZonedDateTime failureTimestamp = ZonedDateTime.now(ZoneOffset.UTC);

  private String failureMessage;

  private String requeueTopic;
}
