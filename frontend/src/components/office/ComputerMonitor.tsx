import type { AgentType } from "../../types";

interface Props {
  deptType: AgentType;
  isExecutive?: boolean;
}

export default function ComputerMonitor({ deptType, isExecutive }: Props) {
  return (
    <div className="relative flex flex-col items-center">
      {/* MONITOR BEZEL & DISPLAY SCREEN */}
      <div
        className={`relative z-20 ${
          isExecutive ? "w-28 h-14" : "w-24 h-11"
        } bg-ink-950 border-2 rounded-t-xl border-slate-700 shadow-2xl p-1.5 flex flex-col justify-between overflow-hidden animate-monitor-glow`}
        style={{
          borderColor: {
            CEO: "rgba(245, 179, 1, 0.7)",
            DEVELOPMENT: "rgba(56, 189, 248, 0.7)",
            MARKETING: "rgba(244, 114, 182, 0.7)",
            FINANCE: "rgba(52, 211, 153, 0.7)",
          }[deptType],
          boxShadow: {
            CEO: "0 0 15px rgba(245, 179, 1, 0.3)",
            DEVELOPMENT: "0 0 15px rgba(56, 189, 248, 0.3)",
            MARKETING: "0 0 15px rgba(244, 114, 182, 0.3)",
            FINANCE: "0 0 15px rgba(52, 211, 153, 0.3)",
          }[deptType],
        }}
      >
        {/* Department-Specific Animated Monitor Screen Content */}
        {deptType === "DEVELOPMENT" && (
          <div className="font-mono text-[7px] leading-tight text-sky-300 opacity-95 overflow-hidden h-full select-none font-semibold">
            <div className="text-pink-400 font-bold">&gt; npm run build</div>
            <div className="text-amber-300">&gt; compiling V1...</div>
            <div className="text-emerald-400">&gt; API READY</div>
            <div className="text-sky-300 animate-pulse">&gt; db connected █</div>
          </div>
        )}

        {deptType === "MARKETING" && (
          <div className="font-mono text-[7px] leading-tight text-pink-300 opacity-95 overflow-hidden h-full flex flex-col justify-between select-none">
            <div className="flex justify-between font-bold border-b border-pink-500/30 pb-0.5">
              <span>CAMPAIGN</span>
              <span className="text-emerald-400">LIVE</span>
            </div>
            <div className="flex justify-between text-[6.5px]">
              <span>CTR: 4.82%</span>
              <span className="text-emerald-300">+18% ↑</span>
            </div>
            <div className="w-full bg-pink-900/60 h-2 rounded overflow-hidden flex items-end">
              <div className="bg-pink-400 w-4/5 h-full animate-pulse" />
            </div>
          </div>
        )}

        {deptType === "FINANCE" && (
          <div className="font-mono text-[7px] leading-tight text-emerald-300 opacity-95 overflow-hidden h-full flex flex-col justify-between select-none">
            <div className="flex justify-between font-bold border-b border-emerald-500/30 pb-0.5">
              <span>BURN:</span>
              <span className="text-amber-300">$12,400</span>
            </div>
            <div className="text-[6.5px] text-slate-200">RUNWAY: 8.3M</div>
            <div className="text-[6.5px] text-emerald-400">REV: $42,800 █</div>
          </div>
        )}

        {deptType === "CEO" && (
          <div className="font-mono text-[7px] leading-tight text-amber-300 opacity-95 overflow-hidden h-full flex flex-col justify-between select-none">
            <div className="font-bold border-b border-amber-500/30 pb-0.5 flex justify-between">
              <span>STRATEGY</span>
              <span className="text-sky-300">V1 MVP</span>
            </div>
            <div className="text-[6.5px] text-slate-200">ROADMAP • KPI</div>
            <div className="text-[6.5px] text-emerald-400">RUNWAY SAFE █</div>
          </div>
        )}
      </div>

      {/* MONITOR STAND NECK & BASE */}
      <div className="w-4 h-3 bg-slate-800 border-x border-slate-700 z-10" />
      <div className="w-10 h-1.5 bg-slate-700 rounded-full z-10 shadow-md border border-slate-600" />
    </div>
  );
}
