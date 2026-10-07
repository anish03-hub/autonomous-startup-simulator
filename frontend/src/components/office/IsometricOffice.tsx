import { useState } from "react";
import type { AgentType } from "../../types";
import { useSim } from "../../store/simulationStore";
import { DEPARTMENTS, DEPT } from "../../lib/theme";
import CEOOfficeRoom from "./CEOOfficeRoom";
import DevelopmentRoom from "./DevelopmentRoom";
import MarketingRoom from "./MarketingRoom";
import FinanceRoom from "./FinanceRoom";
import BoardroomRoom from "./BoardroomRoom";

export default function IsometricOffice({
  onSelect,
}: {
  onSelect: (t: AgentType) => void;
}) {
  const agents = useSim((s) => s.agents);
  const tasks = useSim((s) => s.tasks);
  const messages = useSim((s) => s.messages);
  const startup = useSim((s) => s.startup);
  const debates = useSim((s) => s.debates);

  const phase = startup?.currentPhase;
  const inBoardroom = phase === "DEBATE" || phase === "DECISION";
  const activeDebate = debates.length > 0 ? debates[debates.length - 1] : undefined;

  const [zoomLevel, setZoomLevel] = useState(1.15); // Default zoom level optimized for workstation & employee visibility

  const agentOf = (t: AgentType) => agents.find((a) => a.type === t);
  const taskOf = (t: AgentType) => tasks.filter((x) => x.agentType === t).slice(-1)[0];
  const lastMsgOf = (t: AgentType) =>
    messages.filter((m) => m.agentType === t && m.debateId == null).slice(-1)[0];

  return (
    <div className="iso-scene relative w-full h-full flex items-center justify-center overflow-hidden bg-gradient-to-b from-ink-950 via-ink-900 to-ink-950">
      {/* Zoom / Camera Controls */}
      <div className="absolute top-4 right-4 z-30 flex items-center gap-1 bg-ink-900/90 border border-white/10 p-1 rounded-xl shadow-xl">
        <button
          onClick={() => setZoomLevel((z) => Math.min(z + 0.15, 1.6))}
          className="w-7 h-7 rounded-lg bg-white/5 hover:bg-white/10 text-white font-mono text-sm flex items-center justify-center"
          title="Zoom In"
        >
          +
        </button>
        <button
          onClick={() => setZoomLevel(1.15)}
          className="px-2 h-7 rounded-lg bg-white/5 hover:bg-white/10 text-slate-300 font-mono text-xs flex items-center justify-center"
          title="Reset View"
        >
          Reset
        </button>
        <button
          onClick={() => setZoomLevel((z) => Math.max(z - 0.15, 0.8))}
          className="w-7 h-7 rounded-lg bg-white/5 hover:bg-white/10 text-white font-mono text-sm flex items-center justify-center"
          title="Zoom Out"
        >
          -
        </button>
      </div>

      {/* Main Connected Isometric Headquarters Floor Plane */}
      <div
        className="iso-plane relative transition-transform duration-300"
        style={{
          width: 840,
          height: 840,
          transform: `rotateX(50deg) rotateZ(45deg) scale(${zoomLevel})`,
        }}
      >
        {/* Connected Corridor Hallways */}
        {/* CEO down to Boardroom */}
        <div className="corridor-v absolute" style={{ top: 245, left: 405, width: 30, height: 40 }} />
        {/* Boardroom left to Marketing and right to Development */}
        <div className="corridor-h absolute" style={{ top: 385, left: 260, width: 320, height: 30 }} />
        {/* Boardroom down to Finance */}
        <div className="corridor-v absolute" style={{ top: 525, left: 405, width: 30, height: 40 }} />

        {/* 1. CEO Office (Top Center) */}
        <div className="absolute" style={{ top: 10, left: 280 }}>
          <CEOOfficeRoom
            agent={agentOf("CEO")}
            task={taskOf("CEO")}
            lastMessage={lastMsgOf("CEO")}
            onClick={() => onSelect("CEO")}
          />
        </div>

        {/* 2. Boardroom (Centerpiece) */}
        <div className="absolute" style={{ top: 280, left: 280 }}>
          <BoardroomRoom
            inBoardroom={inBoardroom}
            phase={phase}
            activeDebate={activeDebate}
            onClick={() => {
              const boardroomBtn = document.getElementById("btn-open-boardroom");
              if (boardroomBtn) boardroomBtn.click();
            }}
          />
        </div>

        {/* 3. Marketing Hub (Left Wing) */}
        <div className="absolute" style={{ top: 280, left: 10 }}>
          <MarketingRoom
            agent={agentOf("MARKETING")}
            task={taskOf("MARKETING")}
            lastMessage={lastMsgOf("MARKETING")}
            onClick={() => onSelect("MARKETING")}
          />
        </div>

        {/* 4. Development Bay (Right Wing) */}
        <div className="absolute" style={{ top: 280, left: 550 }}>
          <DevelopmentRoom
            agent={agentOf("DEVELOPMENT")}
            task={taskOf("DEVELOPMENT")}
            lastMessage={lastMsgOf("DEVELOPMENT")}
            onClick={() => onSelect("DEVELOPMENT")}
          />
        </div>

        {/* 5. Finance & Ops (Bottom Center) */}
        <div className="absolute" style={{ top: 550, left: 280 }}>
          <FinanceRoom
            agent={agentOf("FINANCE")}
            task={taskOf("FINANCE")}
            lastMessage={lastMsgOf("FINANCE")}
            onClick={() => onSelect("FINANCE")}
          />
        </div>
      </div>

      {/* Legend & Department Color Indicators */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-4 text-[11px] text-slate-300 bg-ink-900/90 border border-white/10 px-4 py-1.5 rounded-full shadow-xl z-30">
        {DEPARTMENTS.map((t) => (
          <button
            key={t}
            onClick={() => onSelect(t)}
            className="flex items-center gap-1.5 hover:opacity-80 transition"
          >
            <span
              className="h-2.5 w-2.5 rounded-full"
              style={{ backgroundColor: DEPT[t].hex }}
            />
            <span className="font-medium">{DEPT[t].label}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
