package com.volcanoartscenter.platform.shared.messaging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByOpenedByUserIdOrderByLastMessageAtDesc(Long userId, Pageable pageable);

    Page<Conversation> findByStatusInOrderByLastMessageAtDesc(List<ConversationStatus> statuses, Pageable pageable);

    Page<Conversation> findAllByOrderByLastMessageAtDesc(Pageable pageable);

    @Query("""
            SELECT COUNT(DISTINCT c.id) FROM Conversation c
             JOIN Message m ON m.conversationId = c.id
            WHERE c.openedByUserId = :userId
              AND m.senderRole = com.volcanoartscenter.platform.shared.messaging.MessageSenderRole.STAFF
              AND m.readByRecipientAt IS NULL
            """)
    long countThreadsWithUnreadStaffRepliesFor(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(c) FROM Conversation c
            WHERE c.status = com.volcanoartscenter.platform.shared.messaging.ConversationStatus.AWAITING_STAFF
            """)
    long countAwaitingStaff();
}
