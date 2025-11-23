package com.hts.auth.infrastructre.event;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hts.generated.events.AccountCreatedEvent;
import com.hts.generated.events.AccountDeletedEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = Logger.getLogger(KafkaEventConsumer.class);

    @Inject com.hts.auth.domain.service.AccountEventService accountEventService;

    @Incoming("account-created-events")
    public Uni<Void> consumeAccountCreatedEvents(org.eclipse.microprofile.reactive.messaging.Message<byte[]> message) {
        return Uni.createFrom().item(() -> {
                    try {
                        return AccountCreatedEvent.parseFrom(message.getPayload());
                    } catch (InvalidProtocolBufferException e) {
                        LOG.errorf(e, "Failed to parse AccountCreatedEvent");
                        return null;
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(event -> event == null ? Uni.createFrom().voidItem() :
                        accountEventService.createAccount(event.getAccountId(), event.getPassword()))
                .onFailure().retry().atMost(3)
                .onFailure().recoverWithItem(() -> {
                    LOG.errorf("Failed to process AccountCreatedEvent after retries");
                    return null;
                })
                .eventually(() -> Uni.createFrom().completionStage(message.ack()))
                .replaceWithVoid();
    }

    @Incoming("account-deleted-events")
    public Uni<Void> consumeAccountDeletedEvents(org.eclipse.microprofile.reactive.messaging.Message<byte[]> message) {
        return Uni.createFrom().item(() -> {
                    try {
                        return AccountDeletedEvent.parseFrom(message.getPayload());
                    } catch (InvalidProtocolBufferException e) {
                        LOG.errorf(e, "Failed to parse AccountDeletedEvent");
                        return null;
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(event -> event == null ? Uni.createFrom().voidItem() :
                        accountEventService.deleteAccount(event.getAccountId()))
                .onFailure().retry().atMost(3)
                .onFailure().recoverWithItem(() -> {
                    LOG.errorf("Failed to process AccountDeletedEvent after retries");
                    return null;
                })
                .eventually(() -> Uni.createFrom().completionStage(message.ack()))
                .replaceWithVoid();
    }

}
