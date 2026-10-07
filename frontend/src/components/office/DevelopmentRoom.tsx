import type { Agent, AgentTask, AgentMessage } from "../../types";
import Workstation from "./Workstation";

interface Props {
  agent?: Agent;
  task?: AgentTask;
  lastMessage?: AgentMessage;
  onClick: () => void;
}

export default function DevelopmentRoom({ agent, task, lastMessage, onClick }: Props) {
  const state = agent?.state || "IDLE";
  const active = state === "WORKING" || state === "THINKING";

  return (
    <button
      onClick={onClick}
      className={`iso-room group relative text-left rounded-xl border-2 bg-gradient-to-br floor-dev ${
        active ? "border-sky-400 shadow-[0_0_50px_8px_rgba(56,189,248,0.5)] ring-2 ring-sky-400" : "border-sky-500/40 hover:border-sky-400"
      } shadow-2xl transition-all duration-300`}
      style={{ width: 260, height: 240 }}
    >
      {/* Architectural Floor Grid */}
      <div className="room-floor-base absolute inset-0 rounded-xl overflow-hidden opacity-30" />

      {/* Upright Content Plane (Facing Camera) */}
      <div className="iso-upright absolute inset-0 p-3 flex flex-col justify-between">
        {/* Room Header & Small Architectural Signage */}
        <div className="flex items-center justify-between border-b border-sky-500/30 pb-1">
          <div className="flex items-center gap-1.5">
            <span className="text-sm">💻</span>
            <span className="text-xs font-extrabold uppercase tracking-widest text-sky-300 font-mono">
              DEVELOPMENT
            </span>
          </div>
          <span className="text-[9px] font-mono font-bold px-2 py-0.5 rounded bg-sky-500/20 text-sky-300 border border-sky-500/40">
            {state}
          </span>
        </div>

        {/* Room Environment & Workstations */}
        <div className="relative flex-1 grid grid-cols-2 gap-2 items-center my-1">
          {/* Server Rack & Tech Board */}
          <div className="flex flex-col gap-1.5">
            {/* Blinking Server Rack */}
            <div className="bg-ink-950 border border-sky-400/50 rounded p-1 flex items-center justify-between shadow">
              <div className="text-[8px] font-mono text-sky-300">🖥️ SERVER RACK</div>
              <div className="flex gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-led" />
                <span className="w-1.5 h-1.5 rounded-full bg-sky-400 animate-led" />
              </div>
            </div>

            {/* Architecture Board */}
            <div className="bg-ink-950/90 border border-sky-500/30 rounded p-1 text-[7.5px] font-mono text-sky-200 shadow">
              <div className="font-bold border-b border-sky-500/20 pb-0.5 mb-0.5">📐 Architecture</div>
              <div>• React SPA / Vite</div>
              <div>• Spring Boot REST</div>
              <div>• Postgres JPA</div>
            </div>
          </div>

          {/* Seated Developer Workstations */}
          <div className="flex gap-1 justify-center">
            <Workstation
              employee={{
                id: "dev-lead",
                name: agent?.name || "David Park",
                role: "Dev Lead",
                hairColor: "#1e293b",
                shirtColor: "#0284c7",
                skinTone: "#f5d0a9",
                deptType: "DEVELOPMENT",
                state: state,
                speechText: agent?.currentActivity || "💻 Coding MVP",
              }}
            />
            <Workstation
              employee={{
                id: "dev-backend",
                name: "Marcus V.",
                role: "Backend Eng",
                hairColor: "#334155",
                shirtColor: "#0369a1",
                skinTone: "#e0ac69",
                deptType: "DEVELOPMENT",
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
                <span className="text-sky-400 font-bold">{task.progress}%</span>
              </div>
              <div className="h-1 rounded-full bg-black/60 overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-500 bg-sky-400"
                  style={{ width: `${task.progress}%` }}
                />
              </div>
            </div>
          )}

          {lastMessage && (
            <div className="mt-1 text-[8.5px] text-sky-200/90 bg-sky-500/10 px-2 py-0.5 rounded border border-sky-500/30 truncate">
              “{lastMessage.content}”
            </div>
          )}
        </div>
      </div>
    </button>
  );
}
