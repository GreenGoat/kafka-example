package au.com.example.kafka.kafkaconsumerproducerexample.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@EnableKafka
public class KafkaTopicConfiguration {

  @Value("${spring.kafka.deadletter.suffix:-dlq-${spring.application.name}}")
  private String dlqSuffix;

  @Value("${spring.kafka.deadletter.partitions}")
  private Integer dlqPartitions;

  @Value("${spring.kafka.topic.example-topic.name}")
  private String topicName;

  @Value("${spring.kafka.topic.example-topic.partitions}")
  private Integer topicPartitions;

  @Value("${spring.kafka.topic.example-topic.replicas}")
  private Integer topicReplicas;

  @Bean
  public NewTopic paymentCommandTopic() {
    return TopicBuilder.name(topicName)
        .partitions(topicPartitions)
        .replicas(topicReplicas)
        .build();
  }

  @Bean
  public NewTopic paymentCommandDlqTopic() {
    return TopicBuilder.name(topicName + dlqSuffix)
        .partitions(dlqPartitions)
        .replicas(topicReplicas)
        .build();
  }
}
