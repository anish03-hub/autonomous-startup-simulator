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
