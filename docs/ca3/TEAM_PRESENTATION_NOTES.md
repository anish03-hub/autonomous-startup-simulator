# Team Presentation Notes

Use this outline to present the Autonomous Virtual Startup Simulator. Adapt the timing and assign speakers to fit the team.

## Suggested agenda

1. Project overview
2. Main capabilities and agents
3. Architecture and execution flow
4. Development phases and current status
5. Demo plan and discussion

## Project overview

The simulator models an early-stage startup through coordinated virtual departments. A founder provides a startup idea, and specialized agents analyze it and contribute to a startup blueprint. The repository describes a React and TypeScript frontend alongside a Java 21 and Spring Boot backend.

## Department agents

- **CEO:** Sets direction, synthesizes cross-department recommendations, and coordinates strategic decisions.
- **Development:** Proposes architecture, estimates effort, and identifies technical risks.
- **Marketing:** Defines target audiences, positioning, acquisition channels, and growth plans.
- **Finance:** Evaluates budgets, runway, pricing, and unit economics.

## Capabilities to highlight

- Agents reason about their areas of responsibility and can communicate with other agents.
- Agents can use domain tools for calculations and estimation.
- Private agent memory and shared startup memory help preserve context.
- Orchestration can select the next action dynamically rather than following a fixed department sequence.

## Inter-agent communication

Agents can send addressed messages to specific teammates. These messages provide a way to request information, challenge assumptions, and coordinate work across departments. Mention that communication is represented in the simulation rather than being limited to a static sequence of scripted steps.

## Tools and memory

The README lists domain tools for financial calculations, development-effort estimation, and pricing/revenue projections. Agents also have private memory, while shared startup memory can retain context that is useful across departments and simulation steps.

## Dynamic orchestration and replanning

The orchestrator evaluates the simulation state and can choose to run an agent, start a debate, or complete the analysis. When an execution task is blocked, the system can validate a proposed plan change and resume with newly executable work.

## Demonstration modes

- **SCRIPTED_DEMO:** A deterministic, offline option for a repeatable walkthrough without external LLM calls.
- **REAL:** Uses an LLM provider for reasoning; provider failures are reported instead of inventing a result.

For a live presentation, confirm the selected mode and environment configuration before starting.

## Suggested demo flow

1. Introduce a sample startup idea and explain the simulation goal.
2. Show the department agents and their initial analyses.
3. Highlight one agent message or domain-tool result.
4. Show how orchestration chooses a next step, including a debate if one occurs.
5. Summarize the resulting startup blueprint and invite questions.

## Technical overview

The project README identifies Java 21, Spring Boot, Spring Data JPA, and Maven in the backend, and React, TypeScript, and Vite in the frontend. It also describes REST endpoints and server-sent events as part of the application architecture.
