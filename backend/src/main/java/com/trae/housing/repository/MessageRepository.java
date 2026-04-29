package com.trae.housing.repository;

import com.trae.housing.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByReceiverId(Long receiverId);
    List<Message> findBySenderId(Long senderId);
    
    @Query("SELECT m FROM Message m WHERE (m.receiver.id = :userId OR m.sender.id = :userId)")
    List<Message> findByReceiverIdOrSenderId(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.property.id = :propertyId AND ((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR (m.sender.id = :userId2 AND m.receiver.id = :userId1)) ORDER BY m.sentAt ASC")
    List<Message> findChatHistory(@Param("propertyId") Long propertyId, @Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver.id = :receiverId AND m.isRead = false")
    int markAllAsReadByReceiverId(@Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver.id = :receiverId AND m.sender.id = :senderId AND m.property.id = :propertyId AND m.isRead = false")
    int markChatHistoryAsRead(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId,
            @Param("propertyId") Long propertyId
    );
}
