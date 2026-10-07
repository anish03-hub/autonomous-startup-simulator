package com.startupsimulator.service;

import com.startupsimulator.dto.response.AgentMessageDto;
import com.startupsimulator.dto.response.DebateDto;
import com.startupsimulator.dto.response.DecisionDto;
import com.startupsimulator.model.Debate;
import com.startupsimulator.model.enums.DebateStatus;
import com.startupsimulator.repository.AgentMessageRepository;
import com.startupsimulator.repository.DebateRepository;
import com.startupsimulator.repository.DecisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebateService {

    private final DebateRepository debateRepository;
    private final AgentMessageRepository messageRepository;
    private final DecisionRepository decisionRepository;

    @Transactional
    public Debate create(Long startupId, String topic, String question) {
        return debateRepository.save(new Debate(startupId, topic, question));
    }

    @Transactional
    public Debate resolve(Long debateId) {
        return complete(debateId, DebateStatus.RESOLVED);
    }

    /**
     * Close a debate with a specific terminal status (Phase 2C): {@code RESOLVED}
     * when every turn produced a real result, or {@code COMPLETED_WITH_WARNINGS}
     * when at least one agent's LLM turn failed and used a deterministic fallback.
     */
    @Transactional
    public Debate complete(Long debateId, DebateStatus status) {
        Debate debate = debateRepository.findById(debateId).orElseThrow();
        debate.setStatus(status);
        debate.setResolvedAt(Instant.now());
        return debateRepository.save(debate);
    }

    @Transactional(readOnly = true)
    public List<DebateDto> listDtos(Long startupId) {
        Map<Long, DecisionDto> decisionsByDebate = decisionRepository.findByStartupIdOrderById(startupId).stream()
                .filter(d -> d.getDebateId() != null)
                .collect(Collectors.toMap(d -> d.getDebateId(), DecisionDto::from, (a, b) -> a));

        return debateRepository.findByStartupIdOrderById(startupId).stream()
                .map(debate -> {
                    List<AgentMessageDto> messages = messageRepository.findByDebateIdOrderById(debate.getId())
                            .stream().map(AgentMessageDto::from).toList();
                    return DebateDto.from(debate, messages, decisionsByDebate.get(debate.getId()));
                })
                .toList();
    }
}
