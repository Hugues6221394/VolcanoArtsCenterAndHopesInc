package com.volcanoartscenter.platform.shared.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Modifying
    @Query("""
            UPDATE Message m
               SET m.readByRecipientAt = :now
             WHERE m.conversationId = :conversationId
               AND m.senderRole <> :viewerRole
               AND m.readByRecipientAt IS NULL
            """)
    int markIncomingRead(@Param("conversationId") Long conversationId,
                         @Param("viewerRole") MessageSenderRole viewerRole,
                         @Param("now") LocalDateTime now);
}
