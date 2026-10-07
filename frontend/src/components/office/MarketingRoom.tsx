import type { Agent, AgentTask, AgentMessage } from "../../types";
import Workstation from "./Workstation";

interface Props {
  agent?: Agent;
  task?: AgentTask;
  lastMessage?: AgentMessage;
  onClick: () => void;
}

export default function MarketingRoom({ agent, task, lastMessage, onClick }: Props) {
  const state = agent?.state || "IDLE";
  const active = state === "WORKING" || state === "THINKING";

  return (
    <button
      onClick={onClick}
      className={`iso-room group relative text-left rounded-xl border-2 bg-gradient-to-br floor-marketing ${
        active ? "border-pink-400 shadow-[0_0_50px_8px_rgba(244,114,182,0.5)] ring-2 ring-pink-400" : "border-pink-500/40 hover:border-pink-400"
      } shadow-2xl transition-all duration-300`}
      style={{ width: 260, height: 240 }}
    >
      {/* Architectural Floor Grid */}
      <div className="room-floor-base absolute inset-0 rounded-xl overflow-hidden opacity-30" />

      {/* Upright Content Plane (Facing Camera) */}
      <div className="iso-upright absolute inset-0 p-3 flex flex-col justify-between">
        {/* Room Header & Small Architectural Signage */}
        <div className="flex items-center justify-between border-b border-pink-500/30 pb-1">
          <div className="flex items-center gap-1.5">
            <span className="text-sm">📣</span>
            <span className="text-xs font-extrabold uppercase tracking-widest text-pink-300 font-mono">
              MARKETING
            </span>
          </div>
          <span className="text-[9px] font-mono font-bold px-2 py-0.5 rounded bg-pink-500/20 text-pink-300 border border-pink-500/40">
            {state}
          </span>
        </div>

        {/* Room Environment & Workstations */}
        <div className="relative flex-1 grid grid-cols-2 gap-2 items-center my-1">
          {/* Campaign Board & Creative Decor */}
          <div className="flex flex-col gap-1.5">
            <div className="bg-ink-950/90 border border-pink-500/40 rounded p-1 text-[7.5px] font-mono text-pink-200 shadow">
              <div className="font-bold border-b border-pink-500/30 pb-0.5 mb-0.5 flex items-center gap-1">
                <span>🎯</span> CAMPAIGN BOARD
              </div>
              <div>• Persona mapping</div>
              <div>• Freemium funnel</div>
              <div>• Social & referrals</div>
            </div>
            <div className="flex items-center gap-1 text-[9px] font-mono text-pink-300">
              <span>🪴</span> Creative Studio
            </div>
          </div>

          {/* Seated Marketing Workstations */}
          <div className="flex gap-1 justify-center">
            <Workstation
              employee={{
                id: "mkt-lead",
                name: agent?.name || "Sarah J.",
                role: "Marketing Lead",
                hairColor: "#b45309",
                shirtColor: "#db2777",
                skinTone: "#f5d0a9",
                deptType: "MARKETING",
                state: state,
                speechText: agent?.currentActivity || "📣 GTM Campaign",
              }}
            />
            <Workstation
              employee={{
                id: "mkt-growth",
                name: "Maya Lin",
                role: "Growth Spec",
                hairColor: "#1e1b4b",
                shirtColor: "#be185d",
                skinTone: "#c68642",
                deptType: "MARKETING",
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
                <span className="text-pink-400 font-bold">{task.progress}%</span>
              </div>
              <div className="h-1 rounded-full bg-black/60 overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-500 bg-pink-400"
                  style={{ width: `${task.progress}%` }}
                />
              </div>
            </div>
          )}

          {lastMessage && (
            <div className="mt-1 text-[8.5px] text-pink-200/90 bg-pink-500/10 px-2 py-0.5 rounded border border-pink-500/30 truncate">
              “{lastMessage.content}”
            </div>
          )}
        </div>
      </div>
    </button>
  );
}
