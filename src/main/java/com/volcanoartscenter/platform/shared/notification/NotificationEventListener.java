package com.volcanoartscenter.platform.shared.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Routes {@link NotificationEvent}s emitted by domain services to the dispatcher.
 * Listening on AFTER_COMMIT means notifications are only created if the originating
 * business transaction (order creation, booking, etc.) successfully committed.
 */
@Component
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;

    public NotificationEventListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(NotificationEvent event) {
        dispatcher.dispatch(event);
    }
}
