import type { Agent, AgentTask, AgentMessage } from "../../types";
import Workstation from "./Workstation";

interface Props {
  agent?: Agent;
  task?: AgentTask;
  lastMessage?: AgentMessage;
  onClick: () => void;
}

export default function CEOOfficeRoom({ agent, task, lastMessage, onClick }: Props) {
  const state = agent?.state || "IDLE";
  const active = state === "WORKING" || state === "THINKING" || state === "DECIDING";

  return (
    <button
      onClick={onClick}
      className={`iso-room group relative text-left rounded-xl border-2 bg-gradient-to-br floor-ceo ${
        active ? "border-amber-400 shadow-[0_0_50px_8px_rgba(245,179,1,0.5)] ring-2 ring-amber-400" : "border-amber-500/40 hover:border-amber-400"
      } shadow-2xl transition-all duration-300`}
      style={{ width: 260, height: 240 }}
    >
      {/* Architectural Floor Grid */}
      <div className="room-floor-base absolute inset-0 rounded-xl overflow-hidden opacity-30" />

      {/* Upright Content Plane (Facing Camera) */}
      <div className="iso-upright absolute inset-0 p-3 flex flex-col justify-between">
        {/* Room Header & Small Architectural Signage */}
        <div className="flex items-center justify-between border-b border-amber-500/30 pb-1">
          <div className="flex items-center gap-1.5">
            <span className="text-sm">👔</span>
            <span className="text-xs font-extrabold uppercase tracking-widest text-amber-300 font-mono">
              CEO SUITE
            </span>
          </div>
          <span className="text-[9px] font-mono font-bold px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40">
            {state}
          </span>
        </div>

        {/* Room Environment & Workstation */}
        <div className="relative flex-1 grid grid-cols-2 gap-2 items-center my-1">
          {/* Strategy Board & Executive Decor */}
          <div className="flex flex-col gap-1.5">
            <div className="bg-ink-950/90 border border-amber-500/40 rounded p-1.5 text-[8px] font-mono text-amber-200 shadow-md">
              <div className="font-bold border-b border-amber-500/30 pb-0.5 mb-1 flex items-center gap-1">
                <span>📋</span> STRATEGY BOARD
              </div>
              <div className="text-[7.5px] text-slate-300 space-y-0.5">
                <div>• V1 MVP Direction</div>
                <div>• Protect Runway</div>
                <div>• Growth Motion</div>
              </div>
            </div>
            <div className="flex items-center gap-1 text-[9px] font-mono text-slate-300">
              <span>🪴</span> Executive Palm
            </div>
          </div>

          {/* Seated CEO Workstation */}
          <div className="flex justify-center">
            <Workstation
              employee={{
                id: "ceo-lead",
                name: agent?.name || "Alex Vance",
                role: "CEO Lead",
                hairColor: "#4a3b32",
                shirtColor: "#d97706",
                skinTone: "#f5d0a9",
                deptType: "CEO",
                state: state,
                speechText: agent?.currentActivity || "👔 CEO Strategy",
              }}
              isExecutive={true}
            />
          </div>
        </div>

        {/* Task Progress Bar & Message */}
        <div>
          {task && (
            <div className="space-y-0.5 bg-black/50 p-1 rounded border border-white/10">
              <div className="flex justify-between text-[8.5px] font-mono text-slate-300">
                <span className="truncate max-w-[160px]">{task.title}</span>
                <span className="text-amber-400 font-bold">{task.progress}%</span>
              </div>
              <div className="h-1 rounded-full bg-black/60 overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-500 bg-amber-400"
                  style={{ width: `${task.progress}%` }}
                />
              </div>
            </div>
          )}

          {lastMessage && (
            <div className="mt-1 text-[8.5px] text-amber-200/90 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/30 truncate">
              “{lastMessage.content}”
            </div>
          )}
        </div>
      </div>
    </button>
  );
}
