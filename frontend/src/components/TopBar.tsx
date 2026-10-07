import { useSim } from "../store/simulationStore";
import { PHASES } from "../lib/theme";

export default function TopBar({
  sseConnected,
  onOpenBlueprint,
}: {
  sseConnected: boolean;
  onOpenBlueprint: () => void;
}) {
  const startup = useSim((s) => s.startup);
  if (!startup) return null;

  const currentIdx = PHASES.indexOf(startup.currentPhase);

  return (
    <header className="glass border-b border-white/5 px-5 py-3 flex items-center gap-6">
      <div className="flex items-center gap-3 min-w-0">
        <div className="grid place-items-center h-9 w-9 rounded-lg bg-gradient-to-br from-sky-500 to-indigo-600 text-white font-bold">
          {startup.name.slice(0, 1)}
        </div>
        <div className="min-w-0">
          <div className="font-bold text-white leading-tight truncate">
            {startup.name}
          </div>
          <div className="text-[11px] text-slate-400 truncate max-w-[280px]">
            {startup.originalIdea}
          </div>
        </div>
      </div>

      {/* Phase stepper */}
      <div className="hidden md:flex items-center gap-1 flex-1 justify-center">
        {PHASES.map((p, i) => {
          const done = i < currentIdx;
          const active = i === currentIdx;
          return (
            <div key={p} className="flex items-center">
              <div
                className={`px-2.5 py-1 rounded-full text-[11px] font-medium transition ${
                  active
                    ? "bg-sky-500/20 text-sky-300 ring-1 ring-sky-500/50"
                    : done
                    ? "text-emerald-400"
                    : "text-slate-500"
                }`}
              >
                {done ? "✓ " : ""}
                {p.charAt(0) + p.slice(1).toLowerCase()}
              </div>
              {i < PHASES.length - 1 && (
                <span className="w-4 h-px bg-white/10 mx-0.5" />
              )}
            </div>
          );
        })}
      </div>

      <div className="flex items-center gap-3 ml-auto">
        <span className="flex items-center gap-1.5 text-[11px] text-slate-400">
          <span
            className={`h-2 w-2 rounded-full ${
              sseConnected ? "bg-emerald-400 animate-pulse" : "bg-slate-600"
            }`}
          />
          {sseConnected ? "Live" : "Offline"}
        </span>
        <button
          onClick={onOpenBlueprint}
          disabled={!startup.simulationCompleted}
          className="rounded-lg px-4 py-1.5 text-sm font-semibold bg-ceo/20 text-ceo ring-1 ring-ceo/40 hover:bg-ceo/30 disabled:opacity-30 disabled:cursor-not-allowed transition"
        >
          Blueprint
        </button>
      </div>
    </header>
  );
}
