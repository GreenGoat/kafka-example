package au.com.example.kafka.kafkaconsumerproducerexample.listener;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaErrorHandler implements ConsumerAwareListenerErrorHandler {
  private final KafkaErrorService kafkaErrorService;

  @Override
  public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
    kafkaErrorService.handleError(message, exception);
    return null;
  }

  @Override
  public Object handleError(Message<?> message, ListenerExecutionFailedException exception, Consumer<?, ?> consumer) {
    kafkaErrorService.handleError(message, exception);
    return null;
  }
}
