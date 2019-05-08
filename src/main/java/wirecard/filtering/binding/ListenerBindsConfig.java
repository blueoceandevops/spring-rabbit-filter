package wirecard.filtering.binding;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import wirecard.filtering.FilterListener;
import wirecard.filtering.NoFilterListener;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.amqp.core.BindingBuilder.bind;
import static org.springframework.amqp.core.ExchangeBuilder.fanoutExchange;
import static org.springframework.amqp.core.ExchangeBuilder.headersExchange;

@Configuration
public class ListenerBindsConfig {


    private static final int CONCURRENT_CONSUMERS = 2;
    private static final int MAX_CONCURRENT_CONSUMERS = 50;
    private static final long RECEIVE_TIMEOUT = 2000L;
    public static final String EXCHANGE = "wirecard";
    private static final String ALTERNATE_EXCHANGE = "alternate-wirecard";

    private static final String QUEUE_BY_MPA = "filter.queue.mpa";
    private static final String QUEUE_ALL = "filter.queue.all";

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private AmqpAdmin amqpAdmin;

    public Queue buildFilterQueue() {
        Map<String, Object> args = new HashMap();
        return new Queue(QUEUE_BY_MPA, true, false, false, args);
    }

    public Queue buildNoFilterQueue() {
        Map<String, Object> args = new HashMap();
        return new Queue(QUEUE_ALL, true, false, false, args);
    }

    @Bean
    public SimpleMessageListenerContainer filterListenerContainer() {

        Queue filterQueue = buildFilterQueue();
        bindMpaResources(filterQueue);

        SimpleMessageListenerContainer filterListener = new SimpleMessageListenerContainer();
        filterListener.setConnectionFactory(connectionFactory);
        filterListener.setQueues(filterQueue);
        filterListener.setMessageListener(new FilterListener());
        filterListener.setConcurrentConsumers(CONCURRENT_CONSUMERS);
        filterListener.setMaxConcurrentConsumers(MAX_CONCURRENT_CONSUMERS);
        filterListener.setReceiveTimeout(RECEIVE_TIMEOUT);
        filterListener.setDefaultRequeueRejected(false);

        return filterListener;
    }

    @Bean
    public SimpleMessageListenerContainer noFilterListenerContainer() {
        
        Queue noFilterQueue = buildNoFilterQueue();
        bindNoMpaResources(noFilterQueue);

        SimpleMessageListenerContainer noFilterListener = new SimpleMessageListenerContainer();
        noFilterListener.setConnectionFactory(connectionFactory);
        noFilterListener.setQueues(noFilterQueue);
        noFilterListener.setMessageListener(new NoFilterListener());
        noFilterListener.setConcurrentConsumers(CONCURRENT_CONSUMERS);
        noFilterListener.setMaxConcurrentConsumers(MAX_CONCURRENT_CONSUMERS);
        noFilterListener.setReceiveTimeout(RECEIVE_TIMEOUT);
        noFilterListener.setDefaultRequeueRejected(false);

        return noFilterListener;
    }

    private void bindMpaResources(final Queue queue) {
        amqpAdmin.declareQueue(queue);

        Exchange headersExchange = headersExchange(EXCHANGE).
                withArgument("alternate-exchange", ALTERNATE_EXCHANGE).build();

        amqpAdmin.declareExchange(headersExchange);

        Map headers = new HashMap<>();
        headers.put("Mpa", "MPA-001");
        amqpAdmin.declareBinding(bind(queue).to(headersExchange).with("").and(headers));
    }

    private void bindNoMpaResources(final Queue queue) {
        amqpAdmin.declareQueue(queue);
        Exchange fanoutExchange = fanoutExchange(ALTERNATE_EXCHANGE).build();
        amqpAdmin.declareExchange(fanoutExchange);
        amqpAdmin.declareBinding(bind(queue).to(fanoutExchange).with("").noargs());
    }

}
