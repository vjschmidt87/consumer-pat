package br.com.alelo.consumer.consumerpat.service;

import br.com.alelo.consumer.consumerpat.entity.Card;
import br.com.alelo.consumer.consumerpat.entity.Consumer;
import br.com.alelo.consumer.consumerpat.model.exception.CustomException;
import br.com.alelo.consumer.consumerpat.respository.ConsumerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static utils.types.ExceptionsType.CONSUMER_MISSING_DOCUMENT;
import static utils.types.ExceptionsType.CONSUMER_NOT_FOUND;


@Service
public class ConsumerService {

    @Autowired
    MessageService messageService;

    @Autowired
    ConsumerRepository consumerRepository;

    @Autowired
    CardService cardService;

    public void createConsumer(Consumer consumer) {

        consumerRepository.save(consumer);
    }

    public void updateConsumer(Consumer consumer) {

        if (isNull(consumer.getDocumentNumber()) || "".equals(consumer.getDocumentNumber())) {
            throw new CustomException(messageService.get(CONSUMER_MISSING_DOCUMENT.getMessage()),
                    HttpStatus.BAD_REQUEST, CONSUMER_MISSING_DOCUMENT.getCode());
        }

        final Optional<Consumer> checkPersistedConsumer =
                consumerRepository.findConsumerByDocumentNumber(consumer.getDocumentNumber());
        if (checkPersistedConsumer.isEmpty()) {
            throw new CustomException(messageService.get(CONSUMER_NOT_FOUND.getMessage(), "document number",
                    consumer.getDocumentNumber()), HttpStatus.NOT_FOUND,
                    CONSUMER_NOT_FOUND.getCode());
        }

        final Consumer persistedConsumer = checkPersistedConsumer.get();

        consumer.setId(persistedConsumer.getId());

        if (isNotEmpty(consumer.getCards())) {
            final List<Card> consumerPersistedCards = persistedConsumer.getCards();
            final List<Card> discontinuedCards =
                    consumer.getCards().stream().filter(Card::isDiscontinued).collect(Collectors.toList());
            List<Card> newCards = consumer.getCards();
            newCards.removeAll(discontinuedCards);

            //tirando cartões já existentes para travar mudança do saldo
            newCards.removeIf(card -> consumerPersistedCards.stream()
                    .anyMatch(consumerCard -> card.getNumber().equals(consumerCard.getNumber())));

            //atualizando status do cartão
            consumerPersistedCards.forEach(card ->
                    discontinuedCards.stream()
                            .filter(discontinuedCard -> card.getNumber().equals(discontinuedCard.getNumber()))
                            .findFirst().ifPresent(match -> {
                                card.setDiscontinued(true);
                                discontinuedCards.remove(match);
                            }));

            newCards.addAll(consumerPersistedCards);
        }

        consumerRepository.save(consumer);
    }


    public List<Consumer> findAll() {
        return consumerRepository.findAll();
    }

    public Consumer findConsumerById(final Long id) {

        Optional<Consumer> response = consumerRepository.findById(id);

        if (response.isPresent()) {
            return response.get();
        }

        throw new CustomException(messageService.get(CONSUMER_NOT_FOUND.getMessage(), "id", id), HttpStatus.NOT_FOUND,
                CONSUMER_NOT_FOUND.getCode());
    }

    public Consumer findConsumerByDocumentNumber(final String documentNumber) {

        Optional<Consumer> response = consumerRepository.findConsumerByDocumentNumber(documentNumber);

        if (response.isPresent()) {
            return response.get();
        }

        throw new CustomException(messageService.get(CONSUMER_NOT_FOUND.getMessage(), "document number",
                documentNumber), HttpStatus.NOT_FOUND, CONSUMER_NOT_FOUND.getCode());
    }
}