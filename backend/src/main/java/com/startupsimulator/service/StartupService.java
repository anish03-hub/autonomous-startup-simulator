package com.startupsimulator.service;

import com.startupsimulator.dto.request.CreateStartupRequest;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.repository.StartupRepository;
import com.startupsimulator.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Creation and retrieval of startups plus initial agent hiring. */
@Service
@RequiredArgsConstructor
public class StartupService {

    private final StartupRepository startupRepository;
    private final AgentService agentService;
    private final EventService eventService;

    @Transactional
    public Startup createStartup(CreateStartupRequest request) {
        String name = (request.name() == null || request.name().isBlank())
                ? generateName(request.idea())
                : request.name().trim();

        Startup startup = new Startup(name, request.idea().trim());
        final Startup saved = startupRepository.save(startup);

        eventService.record(saved.getId(), EventType.STARTUP_CREATED,
                "Startup \"" + name + "\" founded.",
                Map.of("startupId", saved.getId(), "name", name));

        agentService.initializeAgents(saved).forEach(agent ->
                eventService.record(saved.getId(), EventType.AGENT_INITIALIZED,
                        agent.getName() + " joined the " + agent.getType().getDisplayName() + " department.",
                        Map.of("agentType", agent.getType().name(), "name", agent.getName())));

        return saved;
    }

    @Transactional(readOnly = true)
    public Startup getStartup(Long id) {
        return startupRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Startup", id));
    }

    @Transactional(readOnly = true)
    public List<Startup> listStartups() {
        return startupRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Startup save(Startup startup) {
        return startupRepository.save(startup);
    }

    /**
     * Derive a short, brandable name from the idea (e.g. "skincare" -> "SKNO").
     * Purely cosmetic; the user can override via the request.
     */
    private String generateName(String idea) {
        String[] words = idea.toUpperCase(Locale.ROOT).replaceAll("[^A-Z ]", " ").trim().split("\\s+");
        StringBuilder consonants = new StringBuilder();
        for (String word : words) {
            for (char c : word.toCharArray()) {
                if ("BCDFGHJKLMNPQRSTVWXYZ".indexOf(c) >= 0) {
                    consonants.append(c);
                    if (consonants.length() >= 4) {
                        return consonants.toString();
                    }
                }
            }
        }
        String base = consonants.length() >= 3 ? consonants.toString() : "NOVA";
        return base.length() > 5 ? base.substring(0, 5) : base;
    }
}
