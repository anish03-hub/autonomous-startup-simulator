package com.startupsimulator.service;

import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.repository.AgentMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final AgentMessageRepository messageRepository;

    @Transactional
    public AgentMessage create(Long startupId, AgentType agentType, String content, Long debateId) {
        AgentMessage message = new AgentMessage(startupId, agentType, content);
        message.setDebateId(debateId);
        return messageRepository.save(message);
    }

    /**
     * Create a debate-transcript message with its Phase 2C metadata: which round
     * produced it, the kind of turn (POSITION / CHALLENGE / CONVERGENCE / FRAMING
     * / SYNTHESIS) and, for a CHALLENGE, the department being challenged.
     */
    @Transactional
    public AgentMessage create(Long startupId, AgentType agentType, String content, Long debateId,
                               Integer debateRound, String messageType, AgentType targetAgent) {
        AgentMessage message = new AgentMessage(startupId, agentType, content);
        message.setDebateId(debateId);
        message.setDebateRound(debateRound);
        message.setMessageType(messageType);
        message.setTargetAgent(targetAgent);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<AgentMessage> list(Long startupId) {
        return messageRepository.findByStartupIdOrderById(startupId);
    }

    // ---- Phase 3: addressable retrieval (read-only; no new rows) ------------

    /**
     * All messages addressed TO {@code targetAgent}, chronological. Used by the
     * boardroom to render the "messages addressed to you" section from the
     * persisted store (not the in-memory transcript), exercising the real
     * addressable path. A read only — it never creates a message.
     */
    @Transactional(readOnly = true)
    public List<AgentMessage> addressedTo(Long startupId, AgentType targetAgent) {
        return messageRepository.findByStartupIdAndTargetAgentOrderByIdAsc(startupId, targetAgent);
    }

    /** All messages sent BY {@code sender}, chronological. Read only. */
    @Transactional(readOnly = true)
    public List<AgentMessage> sentBy(Long startupId, AgentType sender) {
        return messageRepository.findByStartupIdAndAgentTypeOrderByIdAsc(startupId, sender);
    }
}
