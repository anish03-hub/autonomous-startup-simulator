package com.startupsimulator.service;

import com.startupsimulator.agent.DecisionSynthesisResponse;
import com.startupsimulator.model.Decision;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.DecisionStatus;
import com.startupsimulator.repository.DecisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final DecisionRepository decisionRepository;

    @Transactional
    public Decision create(Long startupId, Long debateId, String decision, String reason,
                           List<AgentType> affected) {
        Decision d = new Decision(startupId, debateId, decision, reason);
        d.setDecidedBy(AgentType.CEO);
        d.setStatus(DecisionStatus.APPROVED);
        d.setAffectedDepartments(String.join(", ",
                affected.stream().map(AgentType::getDisplayName).toList()));
        return decisionRepository.save(d);
    }

    /**
     * Persist the CEO's structured synthesis of the boardroom debate (Phase 2C)
     * as the final, approved {@link Decision}. The structured fields (final MVP
     * direction, accepted/rejected arguments, final risks and priorities) are
     * stored newline-joined so the blueprint and boardroom view can render them.
     */
    @Transactional
    public Decision createFromSynthesis(Long startupId, Long debateId,
                                        DecisionSynthesisResponse synthesis, List<AgentType> affected) {
        Decision d = new Decision(startupId, debateId, synthesis.decision(), synthesis.rationale());
        d.setDecidedBy(AgentType.CEO);
        d.setStatus(DecisionStatus.APPROVED);
        d.setAffectedDepartments(String.join(", ",
                affected.stream().map(AgentType::getDisplayName).toList()));
        d.setFinalMvpDirection(synthesis.finalMvpDirection());
        d.setAcceptedArguments(joinLines(synthesis.acceptedArguments()));
        d.setRejectedArguments(joinLines(synthesis.rejectedArguments()));
        d.setFinalRisks(joinLines(synthesis.finalRisks()));
        d.setFinalPriorities(joinLines(synthesis.finalPriorities()));
        return decisionRepository.save(d);
    }

    @Transactional(readOnly = true)
    public List<Decision> list(Long startupId) {
        return decisionRepository.findByStartupIdOrderById(startupId);
    }

    private static String joinLines(List<String> items) {
        return items == null || items.isEmpty() ? null : String.join("\n", items);
    }
}
