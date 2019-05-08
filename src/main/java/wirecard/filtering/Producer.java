package wirecard.filtering;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import wirecard.filtering.binding.ListenerBindsConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class Producer {

    @Autowired
    private ConnectionFactory connectionFactory;


    public void publish(String message, boolean filtering) {

        try {
            Channel channel = connectionFactory.createConnection().createChannel(false);
            channel.basicPublish(ListenerBindsConfig.EXCHANGE, "none",
                    filtering ? filteredHeader() : emptyHeader(),
                    message.getBytes());

            System.out.println(" [x] Sent message: '" + message + "'");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AMQP.BasicProperties filteredHeader() {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        Map headers = new HashMap<>();
        headers.put("Mpa", "MPA-001");
        builder.headers(headers);
        builder.deliveryMode(2);
        return builder.build();
    }

    private AMQP.BasicProperties emptyHeader() {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        builder.deliveryMode(2);
        return builder.build();
    }
}
