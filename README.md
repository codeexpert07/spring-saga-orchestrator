# Spring Saga Orchestrator

This project demonstrates a microservices architecture using Spring Boot, Kafka, and Spring State Machine to implement a Saga pattern for distributed transactions. The project is structured as a multi-module Gradle build, promoting modularity and reusability.

## Project Structure

The project consists of the following modules:

-   `common-library`: A shared library containing common domain objects (commands, events), Kafka topic constants, and messaging abstractions (publishers, listeners).
-   `order-service`: Manages order creation and orchestrates the saga using Spring State Machine.
-   `payment-service`: Handles payment processing.
-   `inventory-service`: Manages inventory reservations and releases.
-   `shipping-service`: Handles shipment creation.

## Key Technologies

-   **Spring Boot 3.x**: Framework for building stand-alone, production-grade Spring-based applications.
-   **Apache Kafka**: Distributed streaming platform for building real-time data pipelines and streaming applications.
-   **Spring State Machine**: Framework for building state machine-driven applications, used here for Saga orchestration.
-   **Gradle**: Build automation tool for multi-module projects.
-   **Lombok**: Library to reduce boilerplate code (e.g., getters, setters, constructors).
-   **Jackson**: High-performance JSON processor.

## Setup and Running

To build the project:

```bash
./gradlew clean build
```

To run individual services (e.g., order-service):

```bash
java -jar order-service/build/libs/order-service-1.0-SNAPSHOT.jar
```

*(Note: You will need a running Kafka instance and potentially other infrastructure like a database for full functionality.)*

## Architectural Decisions and Refactorings

During the development of this project, several key architectural decisions and refactorings were implemented to enhance modularity, maintainability, and extensibility:

### 1. Centralized Gradle Build Configuration

The project utilizes a multi-module Gradle setup. The parent `build.gradle` file centralizes common configurations and dependencies, ensuring consistency across all sub-modules.

-   **Common Configurations (`allprojects` block):** `group`, `version`, and `repositories` are defined once at the root level.
-   **Common Java Project Setup (`subprojects` block):** All subprojects automatically apply the `java` plugin and include essential dependencies like Lombok, Jackson, and JUnit for testing. Annotation processor configurations are also handled here.
-   **Spring Boot Specific Configurations (`configure` block):** Spring Boot plugins, dependency management (including Spring Cloud and Testcontainers BOMs), and Spring Boot-specific dependencies are applied only to designated Spring Boot microservices (`order-service`, `payment-service`, `inventory-service`, `shipping-service`). This allows for the inclusion of non-Spring Boot modules (like `common-library`) without inheriting unnecessary Spring dependencies.

### 2. Kafka Abstraction Layer

A significant refactoring was performed to abstract the Kafka-specific implementation details, making the services agnostic to the underlying messaging technology. This allows for easier migration to other messaging systems (e.g., Pulsar) in the future with minimal changes to business logic.

-   **Centralized Kafka Topic Names:** All Kafka topic names are defined as `public static final String` constants in `common-library/src/main/java/com/codeexpert/common/constant/KafkaTopics.java`. This prevents hardcoding and ensures consistency.

-   **Generic Message Publisher (`MessagePublisher`):**
    -   A `MessagePublisher` interface (`common-library/src/main/java/com/codeexpert/common/publisher/MessagePublisher.java`) was introduced, constrained to publish messages of type `T extends DomainEvent`.
    -   A Kafka-specific implementation, `KafkaMessagePublisher` (`common-library/src/main/java/com/codeexpert/common/publisher/KafkaMessagePublisher.java`), uses `KafkaTemplate` to send messages.
    -   All service-level command publishers (e.g., `OrderCommandPublisher`) now depend on the `MessagePublisher` interface, decoupling them from Kafka.

-   **Generic Event Listener (`DomainEventListener`):**
    -   A `DomainEvent` marker interface (`common-library/src/main/java/com/codeexpert/common/event/DomainEvent.java`) was created, which all domain events and commands implement.
    -   A generic `DomainEventListener` interface (`common-library/src/main/java/com/codeexpert/common/listener/DomainEventListener.java`) was defined with an `onEvent(DomainEvent event)` method.
    -   Service-specific listeners (e.g., `OrderEventListener`, `PaymentCommandListener`) implement `DomainEventListener`. They use a functional, map-based dispatch mechanism (or direct `instanceof` check for single-event listeners) to handle specific event types.

-   **Programmatic Kafka Listener Registration (`KafkaListenerRegistrar`):**
    -   A `KafkaListenerRegistrar` (`common-library/src/main/java/com/codeexpert/common/listener/KafkaListenerRegistrar.java`) was created in `common-library`. This component programmatically registers Kafka listeners using Spring's `ConcurrentKafkaListenerContainerFactory`.
    -   This completely removes `@KafkaListener` annotations from the service layers, centralizing Kafka consumer configuration.
    -   Service application classes (e.g., `OrderServiceApplication`) now use a `@PostConstruct` method to inject `KafkaListenerRegistrar` and register their `DomainEventListener` implementations for specific topics and group IDs.

### Integrating a New Service with Kafka Abstraction

When creating a new microservice that needs to interact with Kafka, follow these steps to leverage the established abstraction layer:

1.  **Add `common-library` Dependency:** Ensure your new service's `build.gradle` includes `implementation project(':common-library')`.
2.  **Define Commands/Events:** Create your service-specific command and event classes in `common-library`, ensuring they extend `BaseCommand` or `BaseEvent` respectively.
3.  **Implement `DomainEventListener`:**
    -   Create a listener class (e.g., `MyServiceEventListener`) in your new service.
    -   This class must implement `com.codeexpert.common.listener.DomainEventListener`.
    -   Inject your service's business logic handler (e.g., `MyServiceOrchestrator`).
    -   Implement the `onEvent(DomainEvent event)` method. Inside this method, use `if (event instanceof SpecificEvent)` checks to dispatch to appropriate private handler methods, similar to `OrderEventListener`.
4.  **Use `MessagePublisher`:**
    -   Create a publisher class (e.g., `MyServiceCommandPublisher`) in your new service.
    -   Inject `com.codeexpert.common.publisher.MessagePublisher`.
    -   Define methods for publishing your service's commands/events, delegating to `messagePublisher.publish(topic, key, message)`.
5.  **Register Listeners in Application Class:**
    -   In your service's main `@SpringBootApplication` class (e.g., `MyServiceApplication`), inject `KafkaListenerRegistrar` and your `DomainEventListener` implementation.
    -   Use a `@PostConstruct` method to call `kafkaListenerRegistrar.registerListener()` for each Kafka topic your service needs to consume from. Provide the topic name (from `KafkaTopics`), a unique `groupId`, your `DomainEventListener` instance, and the `Class` of the event type you expect on that topic.

By following these steps, your new service will be fully integrated with the Kafka abstraction, maintaining decoupling from Kafka-specific APIs.

### 3. Port Number Configuration

Each service's port number is configured in its respective `src/main/resources/application.properties` file (e.g., `server.port=8081` for `order-service`).

### 4. Lombok and Class Hierarchy Enhancements

-   Ensured correct usage of Lombok annotations (`@Data`, `@SuperBuilder`, `@EqualsAndHashCode(callSuper = true)`) across `BaseCommand`, `BaseEvent`, and their subclasses to maintain proper inheritance and functionality.
-   Corrected class inheritance for event classes to extend `BaseEvent` and command classes to extend `BaseCommand`, with both `BaseEvent` and `BaseCommand` implementing `DomainEvent`.

## Future Enhancements

-   **Pulsar Implementation:** The abstraction layer is now in place to easily add a Pulsar-specific implementation of `MessagePublisher` and a Pulsar message consumer, allowing for a seamless switch between messaging technologies.
-   **Centralized Error Handling:** Implement a more robust centralized error handling mechanism for Kafka message processing.
-   **Observability:** Integrate advanced monitoring and tracing tools.
