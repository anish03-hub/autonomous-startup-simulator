import { useEffect, useRef } from "react";
import { useSim } from "../../store/simulationStore";
import type { EventType } from "../../types";

const ICON: Partial<Record<EventType, string>> = {
  STARTUP_CREATED: "🚀",
  AGENT_INITIALIZED: "👋",
  TASK_CREATED: "📌",
  AGENT_STARTED_WORK: "⚙️",
  AGENT_PROGRESS_UPDATED: "📈",
  AGENT_MESSAGE_CREATED: "💬",
  DEBATE_STARTED: "⚖️",
  AGENT_JOINED_DEBATE: "🚪",
  AGENT_RESPONDED: "🗣️",
  DECISION_PROPOSED: "💡",
  DECISION_APPROVED: "✅",
  MVP_UPDATED: "🧩",
  BUDGET_UPDATED: "💰",
  PHASE_COMPLETED: "🏁",
  BLUEPRINT_GENERATED: "📘",
  CEO_ANALYSIS_STARTED: "🧠",
  CEO_ANALYSIS_COMPLETED: "🎯",
  CEO_ANALYSIS_FAILED: "⚠️",
  LLM_REQUEST_STARTED: "📡",
  LLM_RESPONSE_RECEIVED: "✨",
  AGENT_ANALYSIS_STARTED: "🔬",
  AGENT_ANALYSIS_COMPLETED: "📋",
  AGENT_ANALYSIS_FAILED: "⚠️",
  DEBATE_ROUND_STARTED: "🔔",
  DEBATE_ROUND_COMPLETED: "➡️",
  AGENT_DEBATE_STARTED: "🎙️",
  AGENT_DEBATE_MESSAGE: "🗯️",
  AGENT_DEBATE_COMPLETED: "☑️",
  DEBATE_COMPLETED: "🤝",
  CEO_SYNTHESIS_STARTED: "🧭",
  CEO_SYNTHESIS_COMPLETED: "📝",
  DECISION_STARTED: "⏳",
  DECISION_COMPLETED: "✅",
};

export default function EventFeed() {
  const events = useSim((s) => s.events);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [events.length]);

  return (
    <div className="glass border border-white/5 rounded-xl flex flex-col flex-1 min-h-0">
      <div className="px-4 py-2.5 border-b border-white/5">
        <h2 className="text-sm font-bold text-white">Activity Feed</h2>
      </div>
      <div className="flex-1 overflow-y-auto scroll-slim p-3 space-y-1.5">
        {events.map((e) => (
          <div
            key={e.id}
            className="flex gap-2 text-[12px] animate-fade-in-up leading-snug"
          >
            <span className="shrink-0">{ICON[e.type] ?? "•"}</span>
            <span className="text-slate-300">{e.message}</span>
          </div>
        ))}
        <div ref={endRef} />
      </div>
    </div>
  );
}
