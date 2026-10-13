package com.library.messaging.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.library.messaging.model.BookReturnedEvent;
import com.library.notification.service.NotificationService;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Message Broker Publisher for Event-Driven Architecture.
 * Publishes events to RabbitMQ topic exchange, with an asynchronous thread-pool fallback.
 */
public class MessagePublisher {
    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);

    private static volatile MessagePublisher instance;
    private final ObjectMapper mapper;
    private final NotificationService notificationService;
    private final ExecutorService asyncWorkerPool = Executors.newFixedThreadPool(4);

    private Connection rabbitConnection;
    private Channel rabbitChannel;
    private boolean rabbitEnabled = true;
    private String exchangeName = "library.events.exchange";
    private String routingKey = "library.event.book.returned";

    private MessagePublisher() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.notificationService = new NotificationService();

        initRabbitMQ();
    }

    private void initRabbitMQ() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("rabbitmq.properties")) {
            if (in != null) props.load(in);
        } catch (Exception ignored) {}

        this.rabbitEnabled = Boolean.parseBoolean(props.getProperty("rabbitmq.enabled", "true"));
        this.exchangeName = props.getProperty("rabbitmq.exchange.name", "library.events.exchange");
        this.routingKey = props.getProperty("rabbitmq.routingKey.bookReturned", "library.event.book.returned");

        if (!rabbitEnabled) {
            logger.info("RabbitMQ is disabled in configuration. Using In-Memory Event Dispatcher.");
            return;
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(props.getProperty("rabbitmq.host", "localhost"));
            factory.setPort(Integer.parseInt(props.getProperty("rabbitmq.port", "5672")));
            factory.setUsername(props.getProperty("rabbitmq.username", "guest"));
            factory.setPassword(props.getProperty("rabbitmq.password", "guest"));
            factory.setVirtualHost(props.getProperty("rabbitmq.virtualHost", "/"));
            factory.setConnectionTimeout(3000);

            this.rabbitConnection = factory.newConnection();
            this.rabbitChannel = rabbitConnection.createChannel();
            this.rabbitChannel.exchangeDeclare(exchangeName, "topic", true);
            logger.info("Connected to RabbitMQ on {}:{} | Exchange: {}", factory.getHost(), factory.getPort(), exchangeName);

        } catch (Exception e) {
            logger.warn("Could not connect to RabbitMQ broker: {}. Falling back to In-Memory Event Dispatcher.", e.getMessage());
            this.rabbitConnection = null;
            this.rabbitChannel = null;
        }
    }

    public static MessagePublisher getInstance() {
        if (instance == null) {
            synchronized (MessagePublisher.class) {
                if (instance == null) {
                    instance = new MessagePublisher();
                }
            }
        }
        return instance;
    }

    /**
     * Publishes a BookReturnedEvent.
     */
    public void publishBookReturnedEvent(BookReturnedEvent event) {
        try {
            String jsonPayload = mapper.writeValueAsString(event);

            // 1. Publish to RabbitMQ broker if connected
            if (rabbitChannel != null && rabbitChannel.isOpen()) {
                rabbitChannel.basicPublish(
                    exchangeName,
                    routingKey,
                    null,
                    jsonPayload.getBytes(StandardCharsets.UTF_8)
                );
                logger.info("📤 [RABBITMQ PUBLISHED] Event ID: {} -> Exchange: '{}', Key: '{}'", 
                        event.getEventId(), exchangeName, routingKey);
            } else {
                logger.info("📤 [EVENT PUBLISHED (LOCAL BUS)] Event ID: {}", event.getEventId());
            }

            // 2. Consume asynchronously by Notification Service
            asyncWorkerPool.submit(() -> {
                try {
                    notificationService.processBookReturned(event);
                } catch (Exception e) {
                    logger.error("Error processing BookReturnedEvent in notification worker", e);
                }
            });

        } catch (Exception e) {
            logger.error("Failed to serialize or publish BookReturnedEvent", e);
        }
    }

    public void close() {
        try {
            if (rabbitChannel != null && rabbitChannel.isOpen()) rabbitChannel.close();
            if (rabbitConnection != null && rabbitConnection.isOpen()) rabbitConnection.close();
            asyncWorkerPool.shutdown();
        } catch (Exception e) {
            logger.warn("Error closing RabbitMQ connection", e);
        }
    }
}
