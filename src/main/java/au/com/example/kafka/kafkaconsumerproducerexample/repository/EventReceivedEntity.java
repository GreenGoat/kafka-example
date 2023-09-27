package au.com.example.kafka.kafkaconsumerproducerexample.repository;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "UNIQUE_EVENT_RECEIVED")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EventReceivedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String eventId;

  private String eventSource;

  private String subject;

  private ZonedDateTime eventTimestamp;

  private ZonedDateTime createdOn;
}
