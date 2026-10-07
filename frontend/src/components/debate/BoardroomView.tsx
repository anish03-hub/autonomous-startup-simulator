import { useEffect, useRef } from "react";
import { useSim } from "../../store/simulationStore";
import { DEPT } from "../../lib/theme";

const ROUND_LABEL: Record<number, string> = {
  1: "Round 1 · Positions",
  2: "Round 2 · Challenges",
  3: "Round 3 · Convergence",
};

// Short badge per debate turn type; the CEO's framing/synthesis get their own.
const TYPE_BADGE: Record<string, string> = {
  FRAMING: "Framing",
  POSITION: "Position",
  CHALLENGE: "Challenge",
  CONVERGENCE: "Revised",
  SYNTHESIS: "Synthesis",
};

export default function BoardroomView({ onClose }: { onClose: () => void }) {
  const debates = useSim((s) => s.debates);
  const debate = debates[debates.length - 1];
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [debate?.messages.length]);

  const warnings = debate?.status === "COMPLETED_WITH_WARNINGS";

  return (
    <div className="absolute inset-0 z-20 grid place-items-center bg-black/60 backdrop-blur-sm p-6">
      <div className="glass w-full max-w-2xl max-h-[85vh] rounded-2xl flex flex-col shadow-2xl">
        <div className="px-5 py-4 border-b border-white/5 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl">⚖️</span>
              <h2 className="font-bold text-white">
                {debate ? debate.topic : "Boardroom"}
              </h2>
              {warnings && (
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/30">
                  ⚠️ completed with warnings
                </span>
              )}
            </div>
            {debate && (
              <p className="text-[12px] text-slate-400 mt-1">{debate.question}</p>
            )}
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white text-xl leading-none"
          >
            ×
          </button>
        </div>

        <div className="flex-1 overflow-y-auto scroll-slim p-5 space-y-3">
          {!debate && (
            <p className="text-sm text-slate-500">
              The boardroom convenes once the analysis phase completes…
            </p>
          )}
          {debate?.messages.map((m, i) => {
            const t = DEPT[m.agentType];
            const prev = debate.messages[i - 1];
            // A round separator whenever the debate round advances.
            const showRound =
              m.debateRound != null &&
              m.debateRound !== (prev?.debateRound ?? null) &&
              m.messageType !== "SYNTHESIS";
            const badge = m.messageType ? TYPE_BADGE[m.messageType] : null;
            return (
              <div key={m.id}>
                {showRound && (
                  <div className="flex items-center gap-2 my-3">
                    <div className="h-px flex-1 bg-white/10" />
                    <span className="text-[10px] uppercase tracking-wider text-slate-500">
                      {ROUND_LABEL[m.debateRound as number] ?? `Round ${m.debateRound}`}
                    </span>
                    <div className="h-px flex-1 bg-white/10" />
                  </div>
                )}
                <div className="flex gap-3 animate-fade-in-up">
                  <div
                    className="shrink-0 h-9 w-9 rounded-full grid place-items-center text-sm border"
                    style={{ borderColor: t.hex, color: t.hex }}
                  >
                    {t.emoji}
                  </div>
                  <div className="rounded-2xl rounded-tl-sm bg-ink-900/80 border border-white/5 px-3.5 py-2 max-w-[85%]">
                    <div className="flex items-center gap-2">
                      <span className={`text-[11px] font-bold ${t.accent}`}>
                        {m.agentLabel}
                      </span>
                      {badge && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded bg-white/5 text-slate-400 uppercase tracking-wide">
                          {badge}
                          {m.targetAgent ? ` → ${DEPT[m.targetAgent].label}` : ""}
                        </span>
                      )}
                    </div>
                    <div className="text-sm text-slate-200 mt-0.5 whitespace-pre-line">
                      {m.content}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
          <div ref={endRef} />
        </div>

        {debate?.decision && (
          <div className="m-4 rounded-xl bg-ceo/10 border border-ceo/40 p-4">
            <div className="flex items-center gap-2 text-ceo font-bold text-sm">
              ✅ Decision — {debate.decision.decidedBy}
            </div>
            <div className="text-sm text-white mt-1">{debate.decision.decision}</div>
            <div className="text-[12px] text-slate-400 mt-1">
              {debate.decision.reason}
            </div>
            {debate.decision.finalMvpDirection && (
              <div className="mt-2 text-[12px]">
                <span className="text-slate-500">What we build: </span>
                <span className="text-slate-200">
                  {debate.decision.finalMvpDirection}
                </span>
              </div>
            )}
            <SynthesisList
              label="Accepted"
              items={debate.decision.acceptedArguments}
              tone="text-emerald-300"
            />
            <SynthesisList
              label="Rejected"
              items={debate.decision.rejectedArguments}
              tone="text-rose-300"
            />
            <SynthesisList
              label="Priorities"
              items={debate.decision.finalPriorities}
              tone="text-sky-300"
            />
            <SynthesisList
              label="Risks"
              items={debate.decision.finalRisks}
              tone="text-amber-300"
            />
            {debate.decision.affectedDepartments.length > 0 && (
              <div className="flex gap-1.5 mt-2 flex-wrap">
                {debate.decision.affectedDepartments.map((d) => (
                  <span
                    key={d}
                    className="text-[10px] px-2 py-0.5 rounded-full bg-white/5 text-slate-300"
                  >
                    {d}
                  </span>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function SynthesisList({
  label,
  items,
  tone,
}: {
  label: string;
  items: string[];
  tone: string;
}) {
  if (!items || items.length === 0) return null;
  return (
    <div className="mt-2">
      <div className={`text-[10px] uppercase tracking-wider font-bold ${tone}`}>
        {label}
      </div>
      <ul className="mt-0.5 space-y-0.5">
        {items.map((it, i) => (
          <li key={i} className="text-[12px] text-slate-300 flex gap-1.5">
            <span className="text-slate-600">•</span>
            <span>{it}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
