package au.com.example.kafka.kafkaconsumerproducerexample;

import au.com.example.kafka.kafkaconsumerproducerexample.listener.CloudEvent;
import au.com.example.kafka.kafkaconsumerproducerexample.listener.KafkaErrorHandler;
import au.com.example.kafka.kafkaconsumerproducerexample.listener.KafkaErrorService;
import au.com.example.kafka.kafkaconsumerproducerexample.repository.EventReceivedEntity;
import au.com.example.kafka.kafkaconsumerproducerexample.repository.EventReceivedRepository;
import au.com.example.kafka.kafkaconsumerproducerexample.repository.FailedEventEntity;
import au.com.example.kafka.kafkaconsumerproducerexample.repository.FailedEventRepository;
import au.com.example.kafka.kafkaconsumerproducerexample.service.ExampleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

@SpringBootTest(webEnvironment = DEFINED_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EmbeddedKafka(
    partitions = 1,
    topics = "${spring.kafka.topic.example-topic.name}",
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
    ports = 9095,
    brokerProperties = {"listeners=PLAINTEXT://localhost:9095", "port=9095"})
class KafkaConsumerProducerExampleApplicationTests {

  private static final int ONE_MINUTE = 1000 * 60;

  @Container
  public static PostgreSQLContainer<?> postgreSQLContainer =
      (PostgreSQLContainer<?>)
          new PostgreSQLContainer("postgres:14.6")
              .withDatabaseName("kafka_example_db")
              .withUsername("sa")
              .withPassword("sa")
              .withFileSystemBind(
                  new File("src/test/resources/docker/init.sh").getAbsolutePath(),
                  "/docker-entrypoint-initdb.d/init.sh",
                  BindMode.READ_ONLY)
              .waitingFor(new HostPortWaitStrategy());
  private String event;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", () -> "sa");
    registry.add("spring.datasource.password", () -> "sa");
  }

  @Value("${spring.kafka.topic.example-topic.name}")
  private String topicName;
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private EventReceivedRepository eventReceivedRepository;
  @Autowired
  private FailedEventRepository failedEventRepository;
  @MockBean
  private ExampleService exampleService;
  @SpyBean
  private KafkaErrorService kafkaErrorService;

  @BeforeEach
  void setUp() throws JsonProcessingException {
    event = toJson(defaultCloudEvent());
  }
















  @Test
  void demoStandardListener() throws ExecutionException, InterruptedException, TimeoutException {
    kafkaTemplate.send(topicName, event)
        .get(30, TimeUnit.SECONDS);

    Mockito.verify(exampleService, Mockito.timeout(ONE_MINUTE)).doSomething(Mockito.any());

    List<EventReceivedEntity> uniqueEventsInTheDb = eventReceivedRepository.findAll();
    uniqueEventsInTheDb.forEach(System.out::println);
  }

  @Test
  void demoReceivingTheSameEventTwice() throws ExecutionException, InterruptedException, TimeoutException {
    Mockito.doAnswer(it -> null).when(exampleService).doSomething(Mockito.any());

    kafkaTemplate.send(topicName, event).get(30, TimeUnit.SECONDS);
    kafkaTemplate.send(topicName, event).get(30, TimeUnit.SECONDS);

    Mockito.verify(exampleService, Mockito.timeout(ONE_MINUTE)).doSomething(Mockito.any());
    Mockito.verify(kafkaErrorService, Mockito.timeout(ONE_MINUTE)).handleError(Mockito.any(), Mockito.any());

    List<EventReceivedEntity> uniqueEventsInTheDb = eventReceivedRepository.findAll();
    uniqueEventsInTheDb.forEach(System.out::println);
  }

  @Test
  void demoServiceException() throws ExecutionException, InterruptedException, TimeoutException {
    Mockito.doThrow(new RuntimeException("Something went wrong")).when(exampleService).doSomething(Mockito.any());

    kafkaTemplate.send(topicName, event).get(30, TimeUnit.SECONDS);

    Mockito.verify(exampleService, Mockito.timeout(ONE_MINUTE)).doSomething(Mockito.any());
    Mockito.verify(kafkaErrorService, Mockito.timeout(ONE_MINUTE)).handleError(Mockito.any(), Mockito.any());

    await().untilAsserted(() -> Assertions.assertThat(failedEventRepository.findAll()).hasSize(1));
    List<FailedEventEntity> all = failedEventRepository.findAll();
    all.forEach(System.out::println);
  }

  @Test
  void demoBadMessage() throws ExecutionException, InterruptedException, TimeoutException {
    kafkaTemplate.send(topicName, """
        {"randomKey":"randomValue"}""").get(30, TimeUnit.SECONDS);
    kafkaTemplate.send(topicName, """
        {"randomKey2":"randomValue2"}""").get(30, TimeUnit.SECONDS);

    await().untilAsserted(() -> Assertions.assertThat(failedEventRepository.findAll()).hasSize(2));
    List<FailedEventEntity> all = failedEventRepository.findAll();
    all.forEach(System.out::println);
  }











  private String toJson(Object obj) throws JsonProcessingException {
    return objectMapper.writeValueAsString(obj);
  }

  private CloudEvent defaultCloudEvent() {
    return new CloudEvent(UUID.randomUUID().toString(), "example", ZonedDateTime.now());
  }

}
