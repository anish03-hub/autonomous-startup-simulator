# Autonomous Virtual Startup Simulator

A multi-agent autonomous startup simulation where CEO, Development, Marketing and Finance agents reason, communicate, use tools, retain persistent memory, dynamically orchestrate work, and adapt their task plan through replanning.

---

## Overview & Agentic Capabilities

The Autonomous Virtual Startup Simulator provides an end-to-end multi-agent simulation platform. Rather than using rigid script pipelines, agents dynamically respond to evolving startup requirements, external constraints, debate outcomes, and execution feedback.

### Key Capabilities Implemented

- **Agent Autonomy / Genuine LLM Reasoning:** Department agents leverage real LLM reasoning to evaluate context, set strategies, make choices, and respond to execution challenges.
- **Inter-Agent Addressed Communication:** Agents send target-addressed messages to each other via structured inboxes to request data, challenge assumptions, and coordinate deliverables.
- **Agent-Driven Tool Selection & Execution:** Agents dynamically discover and invoke domain tools (e.g., financial calculations, effort estimations, revenue projections) based on reasoning context rather than hard-coded agent-to-tool routing.
- **Persistent Agent Memory:** Private agent memory and shared startup memory persist across interactions, allowing historical decisions, message context, and tool outputs to inform future reasoning.
- **Dynamic LLM-Driven Orchestration:** An orchestration agent dynamically evaluates the global startup state to decide the optimal next step (`RUN_AGENT`, `START_DEBATE`, or `COMPLETE_ANALYSIS`) instead of executing a fixed static sequence.
- **Adaptive Execution & Replanning:** When tasks encounter execution feedback, blockers, or failures, agents propose structured task graph modifications that are validated server-side and dynamically executed.
- **Boardroom Debate / Critique / Convergence:** Multi-turn structured debates (`POSITION` → `CHALLENGE` → `CONVERGENCE`) where agents defend recommendations before CEO synthesis.
- **REAL Mode:** Uses genuine LLM reasoning with strict bounds—it does not fabricate decisions when the provider fails.
- **SCRIPTED_DEMO Mode:** Fully deterministic, offline execution with zero external LLM calls, suitable for testing and demonstration.

---

## Current Agent Architecture

The simulator consists of four specialized department agents:

| Agent | Responsibilities |
| :--- | :--- |
| **CEO Agent** | Sets overall vision, synthesizes cross-department strategy, orchestrates high-level goals, evaluates debate convergence, and approves final startup blueprints. |
| **Development Agent** | Architecture design, effort estimation, tech stack selection, core feature breakdown, technical risk identification, and developer task execution. |
| **Marketing Agent** | Target audience definition, channel strategy, acquisition positioning, growth forecasting, competitive analysis, and campaign planning. |
| **Finance Agent** | Financial modeling, budget allocation, runway calculation, pricing structure analysis, burn rate evaluation, and unit economic validation. |

---

## Architecture Flow

```
Startup Context
    ↓
Dynamic Orchestrator
    ↓
Agent execution
    ↓
Communication / Tools / Memory
    ↓
Execution feedback
    ↓
Replanning
    ↓
Task-plan mutation
    ↓
Dynamic orchestration resumes
```

### Architecture Flow Explained

1. **Startup Context:** Holds overall startup state, global parameters, agent registries, shared memory, and active execution graph.
2. **Dynamic Orchestrator:** Evaluates current progress, messages, and task readiness to dynamically select the next operational phase or agent action.
3. **Agent Execution:** The scheduled agent performs reasoning, accesses relevant memory context, and determines appropriate actions.
4. **Communication / Tools / Memory:** Agents exchange addressed messages, invoke domain tools, and record observations into private and shared memory.
5. **Execution Feedback:** Task outputs, blockers, or resource limitations yield feedback for state updates.
6. **Replanning:** Detected blockers or new dependencies trigger structured replan proposals.
7. **Task-Plan Mutation:** The server validates replan proposals, updates dependency graphs, and mutates task states safely.
8. **Dynamic Orchestration Resumes:** The orchestrator re-evaluates the updated graph to schedule newly executable tasks.

---

## Dynamic Orchestration (Phase 6)

Rather than following a fixed `CEO → Development → Marketing → Finance` sequence, the dynamic orchestrator uses LLM reasoning to choose among actions based on the current state of the startup:

- **`RUN_AGENT`**: Selects and runs a specific agent whose task dependencies are met and requires attention.
- **`START_DEBATE`**: Initiates a boardroom discussion when inter-department alignment or strategic tradeoffs require debate.
- **`COMPLETE_ANALYSIS`**: Wraps up the simulation once all key technical, financial, marketing, and leadership milestones have converged into a complete blueprint.

---

## Adaptive Execution & Replanning (Phase 7)

Phase 7 introduces dynamic graph mutation and adaptive execution recovery:

- **Persistent Execution Task Graph:** Tasks and their inter-dependencies are stored in a persistent directed graph.
- **Blocked/Failed Task Feedback:** Runtime failures, missing inputs, or resource constraints produce explicit task feedback (`BLOCKED` or `FAILED`).
- **Structured Replan Proposal:** Agents formulate structured replan proposals containing new tasks, dependency updates, or task deferrals.
- **Server-Side Validation:** All proposed plan mutations undergo strict server-side validation before graph modification.
- **Dependency-Cycle Prevention:** Graph validation prevents cyclic dependencies, ensuring structural integrity.
- **Persisted Task Mutation:** Approved replans update task records and dependency graphs directly in persistent storage.
- **Newly Executable Tasks:** Resolving or deferring dependencies transitions downstream tasks into executable states.
- **Re-Entry into Dynamic Orchestration:** Modified task graphs immediately re-enter Phase 6 dynamic orchestration for scheduling.

---

## Persistent Memory System

- **Private Agent Memory:** Agent-specific history storing past domain observations, task execution context, and internal reasoning.
- **Shared Startup Memory:** Organization-wide memory accessible across agents, storing strategic decisions, product milestones, and budget bounds.
- **Persistent Database Storage:** All memory entities are persisted in database storage (H2 / PostgreSQL) via Spring Data JPA.
- **Memory Retrieval into Reasoning Context:** Relevant memories are retrieved and injected into agent prompts to inform decisions.

---

## Inter-Agent Communication

- **Addressed AgentMessage:** Structured messages explicitly addressed from sender to target recipient.
- **AgentInbox:** Each agent maintains an isolated inbox queue for incoming communications.
- **Persistent Messages:** Messages are saved to the database for full trajectory auditing and context retrieval.
- **Influencing Subsequent Decisions:** Unread and historical messages are fed into reasoning contexts, allowing agents to respond to queries and adjust task execution dynamically.

---

## Domain Tools & Selection

- **Financial Calculator:** Computes runway, burn rate, cash flow, and cost allocations.
- **Development Effort Estimator:** Evaluates technical complexity, developer-month requirements, and delivery milestones.
- **Pricing/Revenue Calculator:** Models unit economics, pricing tiers, CAC, and LTV.
- **LLM Tool Selection:** Agents select tools dynamically based on context and tool descriptions rather than adhering to hard-coded agent-to-tool routing.

---

## Boardroom Debate & Convergence

When strategic tradeoffs or conflicting priorities emerge:
1. **POSITION:** Agents state initial departmental stances backed by domain context.
2. **CHALLENGE:** Agents critique peer positions, pointing out technical, financial, or marketing risks.
3. **CONVERGENCE:** Agents adjust proposals to reconcile feedback and converge on shared recommendations.
4. **CEO Synthesis:** The CEO agent synthesizes final debate arguments, makes authoritative decisions, and updates the strategic roadmap.

---

## Concise Example of Adaptive Behavior

```
Developer task becomes blocked
    ↓
blocker diagnosed
    ↓
replan proposed
    ↓
server validates proposal
    ↓
Finance task added
    ↓
blocked task deferred / dependency updated
    ↓
Phase 6 selects Finance
    ↓
Finance executes
    ↓
orchestration continues
```

---

## REAL Mode vs SCRIPTED_DEMO Mode

- **REAL Mode:**
  - Uses genuine LLM reasoning via `LLMService`.
  - Does not fabricate decisions when the LLM provider fails or encounters errors.
- **SCRIPTED_DEMO Mode:**
  - Deterministic execution with zero external LLM calls.
  - Useful for offline testing, instant verification, and predictable demonstrations.

---

## Technology Stack

### Backend
- **Java 21**
- **Spring Boot**
- **Spring Data JPA**
- **H2 / PostgreSQL**
- **SSE (Server-Sent Events)**
- **Maven**

### Frontend
- **React**
- **TypeScript**
- **Vite**
- **Three.js / React Three Fiber**
- **Zustand**
- **Tailwind CSS**

---

## Testing & Verification Status

- **Backend:** 194/194 tests passing
- **TypeScript:** 0 errors
- **Vite production build:** passing

---

## CA3 Implementation Phase Roadmap

- [x] **Phase 2 — Agent autonomy**
- [x] **Phase 3 — Agent communication**
- [x] **Phase 4 — Tool use**
- [x] **Phase 5 — Persistent memory**
- [x] **Phase 6 — Dynamic orchestration**
- [x] **Phase 7 — Adaptive execution & replanning**
- [ ] **Phase 8 — Robustness and recovery** *(Future work)*
- [ ] **Phase 9 — Execution trace redesign** *(Future work)*

---

## Prerequisites & Running locally

### Prerequisites
- **JDK 21** & **Maven 3.9+**
- **Node 20+** & **npm**

Set `JAVA_HOME` if JDK 21 is not default on path:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

### Run Backend
```bash
cd backend
mvn spring-boot:run
```
- Starts on `http://localhost:8080`
- H2 console at `http://localhost:8080/h2-console` (`jdbc:h2:mem:startupsim`, user `sa`, empty password)

### Run Frontend
```bash
cd frontend
npm install
npm run dev
```
- Starts on `http://localhost:5173`

```
