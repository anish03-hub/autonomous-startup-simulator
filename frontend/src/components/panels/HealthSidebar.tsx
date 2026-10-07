import { useSim } from "../../store/simulationStore";
import { money } from "../../lib/theme";

function Meter({ label, value, hex }: { label: string; value: number; hex: string }) {
  return (
    <div>
      <div className="flex justify-between text-[11px] mb-1">
        <span className="text-slate-400">{label}</span>
        <span className="text-slate-200 font-medium">{value}</span>
      </div>
      <div className="h-1.5 rounded-full bg-black/40 overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-700"
          style={{ width: `${value}%`, backgroundColor: hex }}
        />
      </div>
    </div>
  );
}

export default function HealthSidebar() {
  const startup = useSim((s) => s.startup);
  if (!startup) return null;
  const h = startup.health;

  return (
    <div className="glass border border-white/5 rounded-xl p-4">
      <h2 className="text-sm font-bold text-white mb-3">Startup Health</h2>

      <div className="mb-4">
        <div className="flex items-end justify-between">
          <span className="text-[11px] text-slate-400">Overall progress</span>
          <span className="text-2xl font-extrabold text-white leading-none">
            {h.overallProgress}
            <span className="text-sm text-slate-500">%</span>
          </span>
        </div>
        <div className="h-2 mt-1.5 rounded-full bg-black/40 overflow-hidden">
          <div
            className="h-full rounded-full bg-gradient-to-r from-sky-500 to-emerald-400 transition-all duration-700"
            style={{ width: `${h.overallProgress}%` }}
          />
        </div>
      </div>

      <div className="space-y-2.5">
        <Meter label="MVP progress" value={h.mvpProgress} hex="#38bdf8" />
        <Meter label="Technical feasibility" value={h.technicalFeasibility} hex="#818cf8" />
        <Meter label="Market readiness" value={h.marketReadiness} hex="#f472b6" />
        <Meter label="Financial health" value={h.financialHealth} hex="#34d399" />
      </div>

      <div className="grid grid-cols-2 gap-2 mt-4">
        <div className="rounded-lg bg-ink-900/70 border border-white/5 p-2.5">
          <div className="text-[10px] text-slate-400">Budget left</div>
          <div className="text-lg font-bold text-emerald-300">
            {money(h.budgetRemaining)}
          </div>
        </div>
        <div className="rounded-lg bg-ink-900/70 border border-white/5 p-2.5">
          <div className="text-[10px] text-slate-400">Runway</div>
          <div className="text-lg font-bold text-sky-300">
            {h.runwayMonths.toFixed(1)}
            <span className="text-xs text-slate-500"> mo</span>
          </div>
        </div>
      </div>
    </div>
  );
}
