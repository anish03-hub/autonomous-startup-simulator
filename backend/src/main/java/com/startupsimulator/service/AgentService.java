package com.startupsimulator.service;

import com.startupsimulator.model.Agent;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentState;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Creates and mutates the agent "employees" for a startup. */
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Map<AgentType, String> AGENT_NAMES = Map.of(
            AgentType.CEO, "Ava (CEO)",
            AgentType.DEVELOPMENT, "Devin (Engineering)",
            AgentType.MARKETING, "Mara (Marketing)",
            AgentType.FINANCE, "Finn (Finance)"
    );

    private final AgentRepository agentRepository;

    @Transactional
    public List<Agent> initializeAgents(Startup startup) {
        return List.of(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE)
                .stream()
                .map(type -> {
                    Agent agent = new Agent(startup, type, AGENT_NAMES.getOrDefault(type, type.getDisplayName()));
                    agent.setState(AgentState.IDLE);
                    agent.setCurrentActivity("Waiting for the simulation to start.");
                    return agentRepository.save(agent);
                })
                .toList();
    }

    @Transactional
    public Agent setState(Long startupId, AgentType type, AgentState state, String activity) {
        Agent agent = agentRepository.findByStartupIdAndType(startupId, type)
                .orElseThrow(() -> new IllegalStateException("Agent " + type + " missing for startup " + startupId));
        agent.setState(state);
        if (activity != null) {
            agent.setCurrentActivity(activity);
        }
        return agentRepository.save(agent);
    }

    @Transactional(readOnly = true)
    public List<Agent> findByStartup(Long startupId) {
        return agentRepository.findByStartupIdOrderById(startupId);
    }
}
