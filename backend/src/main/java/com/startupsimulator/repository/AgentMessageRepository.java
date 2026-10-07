package com.startupsimulator.repository;

import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.enums.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {
    List<AgentMessage> findByStartupIdOrderById(Long startupId);
    List<AgentMessage> findByDebateIdOrderById(Long debateId);

    // ---- Phase 3: addressable messaging / inbox retrieval -------------------

    /** Every message addressed to {@code targetAgent}, oldest first (chronological). */
    List<AgentMessage> findByStartupIdAndTargetAgentOrderByIdAsc(Long startupId, AgentType targetAgent);

    /** Unread messages addressed to {@code targetAgent} — the recipient's inbox. */
    List<AgentMessage> findByStartupIdAndTargetAgentAndConsumedFalseOrderByIdAsc(Long startupId, AgentType targetAgent);

    /** Every message sent BY {@code sender}, oldest first (chronological). */
    List<AgentMessage> findByStartupIdAndAgentTypeOrderByIdAsc(Long startupId, AgentType sender);
}
