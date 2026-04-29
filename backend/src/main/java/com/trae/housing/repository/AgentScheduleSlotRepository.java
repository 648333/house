package com.trae.housing.repository;

import com.trae.housing.model.AgentScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AgentScheduleSlotRepository extends JpaRepository<AgentScheduleSlot, Long> {
    List<AgentScheduleSlot> findByAgentIdOrderByStartTimeAsc(Long agentId);
    List<AgentScheduleSlot> findByAgentIdAndAvailableTrueAndStartTimeAfterOrderByStartTimeAsc(Long agentId, LocalDateTime startTime);
}
