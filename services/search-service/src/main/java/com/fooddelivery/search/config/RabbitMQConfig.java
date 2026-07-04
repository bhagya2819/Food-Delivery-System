package com.fooddelivery.search.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "food.delivery.exchange";
    public static final String RESTAURANT_SEARCH_QUEUE = "search.restaurant.sync";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue restaurantSearchQueue() {
        return new Queue(RESTAURANT_SEARCH_QUEUE, true);
    }

    @Bean
    public Binding restaurantSearchBinding() {
        return BindingBuilder.bind(restaurantSearchQueue())
                .to(topicExchange())
                .with("restaurant.#");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
