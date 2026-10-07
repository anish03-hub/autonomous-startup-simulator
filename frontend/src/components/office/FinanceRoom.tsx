import type { Agent, AgentTask, AgentMessage } from "../../types";
import Workstation from "./Workstation";

interface Props {
  agent?: Agent;
  task?: AgentTask;
  lastMessage?: AgentMessage;
  onClick: () => void;
}

export default function FinanceRoom({ agent, task, lastMessage, onClick }: Props) {
  const state = agent?.state || "IDLE";
  const active = state === "WORKING" || state === "THINKING";

  return (
    <button
      onClick={onClick}
      className={`iso-room group relative text-left rounded-xl border-2 bg-gradient-to-br floor-finance ${
        active ? "border-emerald-400 shadow-[0_0_50px_8px_rgba(52,211,153,0.5)] ring-2 ring-emerald-400" : "border-emerald-500/40 hover:border-emerald-400"
      } shadow-2xl transition-all duration-300`}
      style={{ width: 260, height: 240 }}
    >
      {/* Architectural Floor Grid */}
      <div className="room-floor-base absolute inset-0 rounded-xl overflow-hidden opacity-30" />

      {/* Upright Content Plane (Facing Camera) */}
      <div className="iso-upright absolute inset-0 p-3 flex flex-col justify-between">
        {/* Room Header & Small Architectural Signage */}
        <div className="flex items-center justify-between border-b border-emerald-500/30 pb-1">
          <div className="flex items-center gap-1.5">
            <span className="text-sm">📊</span>
            <span className="text-xs font-extrabold uppercase tracking-widest text-emerald-300 font-mono">
              FINANCE
            </span>
          </div>
          <span className="text-[9px] font-mono font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
            {state}
          </span>
        </div>

        {/* Room Environment & Workstations */}
        <div className="relative flex-1 grid grid-cols-2 gap-2 items-center my-1">
          {/* Budget Board & Vault */}
          <div className="flex flex-col gap-1.5">
            <div className="bg-ink-950/90 border border-emerald-500/40 rounded p-1 text-[7.5px] font-mono text-emerald-200 shadow">
              <div className="font-bold border-b border-emerald-500/30 pb-0.5 mb-0.5 flex items-center gap-1">
                <span>💰</span> BUDGET BOARD
              </div>
              <div>• Monthly burn rate</div>
              <div>• Upfront capital</div>
              <div>• Runway projection</div>
            </div>
            <div className="flex items-center gap-1 text-[9px] font-mono text-emerald-300">
              <span>🏦</span> Financial Vault
            </div>
          </div>

          {/* Seated Finance Workstations */}
          <div className="flex gap-1 justify-center">
            <Workstation
              employee={{
                id: "fin-lead",
                name: agent?.name || "Robert T.",
                role: "Finance Director",
                hairColor: "#475569",
                shirtColor: "#059669",
                skinTone: "#f5d0a9",
                deptType: "FINANCE",
                state: state,
                speechText: agent?.currentActivity || "📊 Calculating Burn",
              }}
            />
            <Workstation
              employee={{
                id: "fin-analyst",
                name: "James M.",
                role: "Controller",
                hairColor: "#1e293b",
                shirtColor: "#047857",
                skinTone: "#8d5524",
                deptType: "FINANCE",
                state: state,
              }}
            />
          </div>
        </div>

        {/* Task Progress Bar & Message */}
        <div>
          {task && (
            <div className="space-y-0.5 bg-black/50 p-1 rounded border border-white/10">
              <div className="flex justify-between text-[8.5px] font-mono text-slate-300">
                <span className="truncate max-w-[160px]">{task.title}</span>
                <span className="text-emerald-400 font-bold">{task.progress}%</span>
              </div>
              <div className="h-1 rounded-full bg-black/60 overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-500 bg-emerald-400"
                  style={{ width: `${task.progress}%` }}
                />
              </div>
            </div>
          )}

          {lastMessage && (
            <div className="mt-1 text-[8.5px] text-emerald-200/90 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/30 truncate">
              “{lastMessage.content}”
            </div>
          )}
        </div>
      </div>
    </button>
  );
}
