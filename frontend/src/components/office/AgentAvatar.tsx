import type { Agent } from "../../types";
import { STATE_DOT, STATE_LABEL } from "../../lib/theme";

/** A seated AI employee. Bobs while working; pulses a status ring. */
export default function AgentAvatar({ agent }: { agent: Agent }) {
  const active =
    agent.state === "WORKING" ||
    agent.state === "THINKING" ||
    agent.state === "DECIDING";

  return (
    <div className="flex flex-col items-center gap-1 w-20">
      <div className="relative">
        {active && (
          <span
            className={`absolute inset-0 rounded-full ${STATE_DOT[agent.state]} opacity-40 animate-pulse-ring`}
          />
        )}
        <div
          className={`relative grid place-items-center h-11 w-11 rounded-full bg-ink-700 border-2 border-white/20 text-lg ${
            active ? "animate-bob" : ""
          }`}
          title={`${agent.name} — ${STATE_LABEL[agent.state]}`}
        >
          {avatarGlyph(agent)}
          <span
            className={`absolute -bottom-0.5 -right-0.5 h-3.5 w-3.5 rounded-full border-2 border-ink-850 ${STATE_DOT[agent.state]}`}
          />
        </div>
      </div>
      <div className="text-center leading-tight">
        <div className="text-[11px] font-semibold text-white truncate max-w-[80px]">
          {agent.name}
        </div>
        <div className="text-[9px] uppercase tracking-wide text-slate-400">
          {STATE_LABEL[agent.state]}
        </div>
      </div>
    </div>
  );
}

function avatarGlyph(agent: Agent) {
  switch (agent.type) {
    case "CEO":
      return "🧑‍💼";
    case "DEVELOPMENT":
      return "🧑‍💻";
    case "MARKETING":
      return "🧑‍🎨";
    case "FINANCE":
      return "🧑‍🔬";
  }
}
