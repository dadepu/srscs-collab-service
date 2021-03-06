package de.danielkoellgen.srscscollabservice.events.consumer.deckcards;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.danielkoellgen.srscscollabservice.domain.collaboration.application.CollaborationService;
import de.danielkoellgen.srscscollabservice.domain.collaborationcard.application.CollaborationCardService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class KafkaDeckCardsEventConsumer {

    private final CollaborationService collaborationService;
    private final CollaborationCardService collaborationCardService;

    private final Logger logger = LoggerFactory.getLogger(KafkaDeckCardsEventConsumer.class);

    @Autowired
    public KafkaDeckCardsEventConsumer(CollaborationService collaborationService,
            CollaborationCardService collaborationCardService) {
        this.collaborationService = collaborationService;
        this.collaborationCardService = collaborationCardService;
    }

    @KafkaListener(topics = {"${kafka.topic.deckscards}"}, id = "${kafka.groupId.deckscards}")
    public void receive(@NotNull ConsumerRecord<String, String> event) throws JsonProcessingException {
        String eventName = getHeaderValue(event, "type");
        switch (eventName) {
            case "deck-created"     -> processDeckCreatedEvent(event);
            case "deck-disabled"    -> processDeckDisabledEvent(event);
            case "card-created"     -> processCardCreatedEvent(event);
            case "card-overridden"  -> processCardOverriddenEvent(event);
            case "card-disabled"    -> processCardDisabledEvent(event);
            default -> {
                logger.trace("Received event on 'cdc.decks-cards.0' of unknown type '{}'.", eventName);
                throw new RuntimeException("Received event on 'cdc.decks-cards.0' of unknown type '"+eventName+"'.");
            }
        }
    }

    private void processDeckCreatedEvent(@NotNull ConsumerRecord<String, String> event) throws JsonProcessingException {
        DeckCreated deckDisabled = new DeckCreated(collaborationService, event);
        logger.trace("Received 'DeckCreated' event. [tid={}, payload={}]",
                deckDisabled.getTransactionId(), deckDisabled);
        deckDisabled.execute();
    }

    private void processDeckDisabledEvent(@NotNull ConsumerRecord<String, String> event) throws JsonProcessingException {
        DeckDisabled deckDisabled = new DeckDisabled(event);
        logger.trace("Received 'DeckDisabled' event. [tid={}, payload={}]",
                deckDisabled.getTransactionId(), deckDisabled);
        deckDisabled.execute();
    }

    private void processCardCreatedEvent(@NotNull ConsumerRecord<String, String> event) throws JsonProcessingException {
        CardCreated cardCreated = new CardCreated(collaborationCardService, event);
        logger.trace("Received 'CardCreated' event. [tid={}, payload={}]",
                cardCreated.getTransactionId(), cardCreated);
        cardCreated.execute();
    }

    private void processCardOverriddenEvent(@NotNull ConsumerRecord<String, String> event) throws JsonProcessingException {
        CardOverridden cardOverridden = new CardOverridden(collaborationCardService, event);
        logger.trace("Received 'CardOverridden' event. [tid={}, payload={}]",
                cardOverridden.getTransactionId(), cardOverridden);
        cardOverridden.execute();
    }

    private void processCardDisabledEvent(@NotNull ConsumerRecord<String, String> event) throws JsonProcessingException {
        CardDisabled cardDisabled = new CardDisabled(event);
        logger.trace("Received 'CardDisabled' event. [tid={}, payload={}]",
                cardDisabled.getTransactionId(), cardDisabled);
        cardDisabled.execute();
    }

    public static String getHeaderValue(ConsumerRecord<String, String> event, String key) {
        return new String(event.headers().lastHeader(key).value(), StandardCharsets.US_ASCII);
    }
}
