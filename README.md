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

# CA3 Development Phases

This section documents the complete project progression from Phase 1 through Phase 9.

| Phase | Capability | Completed By | Status |
|---|---|---|---|
| Phase 1 | Foundation & Initial Simulation | [@Sujitsingh9832](https://github.com/Sujitsingh9832) | ✅ COMPLETED |
| Phase 2 | Agent Autonomy | [@anish03-hub](https://github.com/anish03-hub) | ✅ COMPLETED |
| Phase 3 | Inter-Agent Communication | [@Pratikchy](https://github.com/Pratikchy) | ✅ COMPLETED |
| Phase 4 | Agent-Driven Tool Use | [@Sujitsingh9832](https://github.com/Sujitsingh9832) | ✅ COMPLETED |
| Phase 5 | Persistent Agent Memory | [@anish03-hub](https://github.com/anish03-hub) | ✅ COMPLETED |
| Phase 6 | Dynamic Orchestration | [@Pratikchy](https://github.com/Pratikchy) | ✅ COMPLETED |
| Phase 7 | Adaptive Execution & Replanning | [@Sujitsingh9832](https://github.com/Sujitsingh9832) | ✅ COMPLETED |
| Phase 8 | Robustness & Recovery | — | ⏳ NOT STARTED |
| Phase 9 | Execution Trace | — | ⏳ NOT STARTED |

---

## Phase 1 — Foundation & Initial Simulation

**Completed by:** [@Sujitsingh9832](https://github.com/Sujitsingh9832)

**Status:** ✅ COMPLETED

### Details

Established the core simulation architecture and user experience foundation:

- Founder enters a startup idea
- CEO department
- Development department
- Marketing department
- Finance department
- Initial startup orchestration
- Initial agent analysis flow
- Boardroom/debate experience
- React + TypeScript + Vite frontend
- Spring Boot / Java 21 backend
- REST APIs
- SSE event streaming
- JPA persistence
- H2/PostgreSQL support
- Isometric office interface
- Initial deterministic/mock AI reasoning architecture

Phase 1 established the foundational infrastructure that subsequent phases extended into a genuinely agentic architecture.

---

## Phase 2 — Agent Autonomy

**Completed by:** [@anish03-hub](https://github.com/anish03-hub)

**Status:** ✅ COMPLETED

### Details

Implemented autonomous reasoning and real LLM integration:

- Genuine department-level LLM reasoning
- Typed agent analysis responses
- CEO synthesis
- Boardroom Debate (`POSITION` → `CHALLENGE` → `CONVERGENCE`)
- `REAL` mode
- `SCRIPTED_DEMO` mode
- Explicit provider failure handling
- Authoritative persistence
- Structured validation
- Real LLM reasoning replacing fixed mock reasoning

---

## Phase 3 — Inter-Agent Communication

**Completed by:** [@Pratikchy](https://github.com/Pratikchy)

**Status:** ✅ COMPLETED

### Details

Implemented target-addressed inter-agent communication:

- Addressable `AgentMessage` (Sender, Recipient, Subject, Content, CreatedAt, Consumed/read state)
- `AgentInbox` with recipient validation
- Persistent agent messages saved in relational storage
- Communication events (`AGENT_MESSAGE_SENT`, `AGENT_MESSAGE_RECEIVED`, `AGENT_MESSAGE_FAILED`)
- Messages included in agent reasoning context to influence decisions
- Cross-department agent communication

---

## Phase 4 — Agent-Driven Tool Use

**Completed by:** [@Sujitsingh9832](https://github.com/Sujitsingh9832)

**Status:** ✅ COMPLETED

### Details

#### Phase 4A — Tool Infrastructure
- `ToolRegistry` and `ToolExecutionService`
- Tool schema definition and validation
- `ToolRequest` and `ToolResult` data structures
- Domain tools: `financial_calculator`, `development_effort_estimator`, `pricing_revenue_calculator`
- Bounded tool execution
- Tool success and failure events

#### Phase 4B — LLM Tool Selection & Execution
- `AgentToolReasoner` integration
- LLM dynamically chooses which tool to use
- LLM generates valid tool arguments based on context
- Actual registered tool executes on the backend
- Actual result is returned to the agent
- Agent reasons using the returned tool outputs
- No hard-coded agent-to-tool routing

---

## Phase 5 — Persistent Agent Memory

**Completed by:** [@anish03-hub](https://github.com/anish03-hub)

**Status:** ✅ COMPLETED

### Details

#### Phase 5A — Persistent Storage Infrastructure
- Persistent `AgentMemory` models
- `AGENT_PRIVATE` memory and `STARTUP_SHARED` memory
- `MemoryService` interface and Spring Data JPA persistence
- Typed memory queries, Importance ratings, Source/sourceReference tracking
- Deterministic deduplication
- Startup isolation and agent isolation
- Cross-run persistence across system restarts

#### Phase 5B — Context Retrieval & Reasoning Integration
- `AgentMemoryContextBuilder`
- Automatic relevant-memory retrieval
- Private memory prioritized before shared memory
- Importance and recency ranking
- Bounded memory context limits
- Memory injected directly into reasoning prompts
- LLM-generated memory intents
- Bounded memory creation (created only after successful reasoning)
- Verified cross-run persistence proof

---

## Phase 6 — Dynamic Orchestration

**Completed by:** [@Pratikchy](https://github.com/Pratikchy)

**Status:** ✅ COMPLETED

### Details

#### Phase 6A — Decision Engine & State Snapshots
- `OrchestrationDecisionEngine` and `OrchestrationStateSnapshot`
- Snapshot factory capturing current simulation state, completed/pending agents, recent messages, persistent memory, and past decisions
- Typed orchestration decisions with server validation
- Genuine LLM-driven decision making
- Available actions:
  - `RUN_AGENT`
  - `START_DEBATE`
  - `COMPLETE_ANALYSIS`

#### Phase 6B — Dynamic Runtime Execution
- Dynamic runtime orchestration with bounded loop execution
- State rebuilt dynamically after every action
- Dynamic agent ordering — no fixed `CEO → Development → Marketing → Finance` sequence
- Actual selected agent executes based on state evaluation
- Inter-agent communication and persistent memory affect future orchestration choices
- Explicit runtime termination with maximum orchestration-step bound
- Deterministic `SCRIPTED_DEMO` fallback path
- Verified dynamic-order proof where Finance can be selected and executed directly while Development and Marketing do not execute

---

## Phase 7 — Adaptive Execution & Replanning

**Completed by:** [@Sujitsingh9832](https://github.com/Sujitsingh9832)

**Status:** ✅ COMPLETED

### Details

Runtime execution graph adaptation and task replanning:

- Persistent `ExecutionTask` graph (ownership, priority, dependencies, lifecycle)
- Task lifecycle states: `PENDING`, `IN_PROGRESS`, `BLOCKED`, `COMPLETED`, `DEFERRED`, `CANCELLED`
- Structured `ReplanProposal` with `ReplanActionType` (`ADD_TASK`, `MODIFY_TASK`, `DEFER_TASK`, `CANCEL_TASK`, `CHANGE_OWNER`, `ADD_DEPENDENCY`, `REMOVE_DEPENDENCY`)
- `ReplanValidator`, `TaskPlanMutationService`, and `ReplanEngine`
- LLM-generated replanning with server-side validation and dependency-cycle detection
- Bounded replanning limits and persisted database graph mutations

#### Complete Adaptive Cycle Flow

```
Agent executes task
    ↓
Task becomes blocked
    ↓
Blocker is diagnosed
    ↓
Replan is requested
    ↓
LLM proposes plan change
    ↓
Server validates proposal
    ↓
Task graph is mutated
    ↓
New orchestration snapshot is created
    ↓
New task becomes executable
    ↓
Phase 6 selects the next agent/task
    ↓
Execution continues
```

#### Actual Finance Adaptive Example
- Developer task becomes blocked because financial feasibility has not been evaluated.
- The replan proposal:
  - Adds a Finance validation task
  - Defers the blocked Development task
  - Adds the Finance task as a dependency for Development
- Finance task becomes executable, and the Phase 6 dynamic orchestrator selects it for execution.

#### Subsystem Integration & Validation
- Fully integrated with `AgentMessage`, `AgentInbox`, persistent memory, `REAL` mode, and `SCRIPTED_DEMO` mode.
- Replan event streaming and safety bounds.
- Current system verification: `194/194` backend tests passing, `0` TypeScript errors, `passing` Vite production build.

---

## Phase 8 — Robustness & Recovery

**Completed by:** —

**Status:** ⏳ NOT STARTED

### Details

This phase is future work.

Planned scope:
- Failure classification
- Bounded retries
- Provider failure recovery
- Tool failure recovery
- Agent execution failure recovery
- Replan failure handling
- Duplicate execution prevention
- Graceful termination
- Circuit breakers where appropriate

---

## Phase 9 — Execution Trace

**Completed by:** —

**Status:** ⏳ NOT STARTED

### Details

This phase is future work.

Planned scope:
- Richer execution trace
- Agent decision history
- Tool invocation history
- Communication history
- Memory retrieval history
- Orchestration decisions
- Replanning decisions
- Task lifecycle trace
- Evaluator-friendly execution evidence

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
