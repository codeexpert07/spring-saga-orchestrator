package org.codeexpert.order;

import org.codeexpert.common.command.*;
import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.listener.KafkaListenerRegistrar;
import org.codeexpert.common.publisher.KafkaMessagePublisher;
import org.codeexpert.common.publisher.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.codeexpert.order.config.OrderStateMachineConfig;
import org.codeexpert.order.model.OrderEvent;
import org.codeexpert.order.model.OrderState;
import org.codeexpert.order.service.OrderCommandPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
@EnableStateMachine
@Import(OrderStateMachineConfig.class)
public class TestConfig {


    @Bean
    public DataSource dataSource() {
        org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setShowSql(true);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.codeexperts.order.persistence");
        factory.setDataSource(dataSource());
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return txManager;
    }


    @Bean
    public StateMachineService<OrderState, OrderEvent> stateMachineService(
            StateMachineFactory<OrderState, OrderEvent> stateMachineFactory) {
        return new DefaultStateMachineService<>(stateMachineFactory);
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9095");
        String bootstrapServers = (String) configs.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setAutoCreate(true);

        System.out.println("✓ Order Service KafkaAdmin configured with bootstrap servers: " + bootstrapServers);
        return admin;
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return createTopic(KafkaTopics.PAYMENT_EVENTS);
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return createTopic(KafkaTopics.INVENTORY_EVENTS);
    }

    @Bean
    public NewTopic shippingEventsTopic() {
        return createTopic(KafkaTopics.SHIPPING_EVENTS);
    }

    @Bean
    @Primary
    public MessagePublisher messagePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaMessagePublisher();
    }

    @Bean
    @Primary
    public KafkaListenerRegistrar kafkaListenerRegistrar(
            ConcurrentKafkaListenerContainerFactory<String, Object> factory,
            ObjectMapper objectMapper) {
        return new KafkaListenerRegistrar(factory, objectMapper);
    }
    
    @Bean
    public OrderCommandPublisher orderCommandPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new OrderCommandPublisher() {
            @Override
            public void publishPaymentCommand(ProcessPaymentCommand command) {
                kafkaTemplate.send(KafkaTopics.PAYMENT_COMMANDS, command.getOrderId(), command);
            }

            @Override
            public void publishRefundCommand(RefundPaymentCommand command) {
                kafkaTemplate.send(KafkaTopics.PAYMENT_COMMANDS, command.getOrderId(), command);
            }

            @Override
            public void publishInventoryCommand(ReserveInventoryCommand command) {
                kafkaTemplate.send(KafkaTopics.INVENTORY_COMMANDS, command.getOrderId(), command);
            }

            @Override
            public void publishReleaseInventoryCommand(ReleaseInventoryCommand command) {
                kafkaTemplate.send(KafkaTopics.INVENTORY_COMMANDS, command.getOrderId(), command);
            }

            @Override
            public void publishShippingCommand(CreateShipmentCommand command) {
                kafkaTemplate.send(KafkaTopics.SHIPPING_COMMANDS, command.getOrderId(), command);
            }
        };
    }

    private NewTopic createTopic(String topicName) {
        return new NewTopic(topicName, 1, (short) 1);
    }
}
