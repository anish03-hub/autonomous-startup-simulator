import { useSim } from "../../store/simulationStore";
import { money } from "../../lib/theme";
import type { MvpFeatureDetail, ReasoningStep, BoardroomDecisionItem } from "../../types";

function SourceBadge({ source }: { source?: string }) {
  if (!source) return null;
  let color = "bg-purple-500/10 text-purple-300 border-purple-500/30";
  const s = source.toUpperCase();
  if (s.includes("DEVELOPMENT")) {
    color = "bg-sky-500/10 text-sky-300 border-sky-500/30";
  } else if (s.includes("MARKETING")) {
    color = "bg-pink-500/10 text-pink-300 border-pink-500/30";
  } else if (s.includes("FINANCE")) {
    color = "bg-emerald-500/10 text-emerald-300 border-emerald-500/30";
  } else if (s === "CEO") {
    color = "bg-amber-500/10 text-amber-300 border-amber-500/30";
  }

  return (
    <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${color} inline-flex items-center gap-1`}>
      <span>Source:</span>
      <span className="font-semibold">{source}</span>
    </span>
  );
}

function Section({
  id,
  title,
  source,
  children,
}: {
  id?: string;
  title: string;
  source?: string;
  children: React.ReactNode;
}) {
  return (
    <section id={id} className="mb-8 scroll-mt-6">
      <div className="flex items-center justify-between mb-3 border-b border-white/5 pb-2">
        <h3 className="text-xs font-bold uppercase tracking-widest text-sky-400 flex items-center gap-2">
          {title}
        </h3>
        <SourceBadge source={source} />
      </div>
      {children}
    </section>
  );
}

function FeatureCard({ f, isMvp }: { f: MvpFeatureDetail; isMvp: boolean }) {
  return (
    <div className="flex items-start gap-3 rounded-xl bg-ink-900/70 border border-white/5 p-3.5 hover:border-white/10 transition">
      <span
        className={`text-[10px] font-mono font-bold mt-0.5 px-2 py-0.5 rounded ${
          isMvp ? "bg-emerald-500/15 text-emerald-400 border border-emerald-500/30" : "bg-slate-800 text-slate-400 border border-slate-700"
        }`}
      >
        {f.targetRelease || (isMvp ? "V1" : "V2")}
      </span>
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <div className="text-sm font-semibold text-white">{f.name}</div>
          <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/5 text-slate-400">{f.priority}</span>
        </div>
        <div className="text-xs text-slate-300 mt-1">{f.description}</div>
        {f.reason && (
          <div className="text-[11px] text-slate-400 mt-1.5 italic bg-black/20 px-2.5 py-1 rounded">
            💡 {f.reason}
          </div>
        )}
      </div>
      <div className="text-right shrink-0">
        <span className="text-[10px] text-slate-500 font-mono">effort: {f.effort}/5</span>
        <div className="mt-1">
          <SourceBadge source={f.sourceDepartment} />
        </div>
      </div>
    </div>
  );
}

export default function BlueprintView({ onClose }: { onClose: () => void }) {
  const bp = useSim((s) => s.blueprint);
  const executionState = useSim((s) => s.executionState);
  const startExecution = useSim((s) => s.startExecution);

  const handleStartExecution = async () => {
    await startExecution();
    onClose();
  };

  return (
    <div className="absolute inset-0 z-40 bg-ink-950/95 overflow-y-auto scroll-slim select-none">
      <div className="max-w-4xl mx-auto px-6 py-8">
        {/* Top bar */}
        <div className="flex items-center justify-between mb-6 border-b border-white/10 pb-4">
          <div className="flex items-center gap-3">
            <span className="text-xl">📘</span>
            <div>
              <div className="text-xs font-mono uppercase tracking-widest text-ceo font-bold">
                Traceable Startup Blueprint
              </div>
              <div className="text-[11px] text-slate-400">
                Autonomous agent reasoning & boardroom decision artifact
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {!executionState?.isRunning && (
              <button
                onClick={() => void handleStartExecution()}
                className="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-bold text-xs shadow-md shadow-emerald-500/20 transition transform hover:scale-105 flex items-center gap-1.5"
              >
                🚀 Start Autonomous Execution
              </button>
            )}
            <button
              onClick={onClose}
              className="rounded-lg px-3.5 py-1.5 text-sm bg-white/5 hover:bg-white/10 text-slate-200 border border-white/10 transition"
            >
              ← Back to office
            </button>
          </div>
        </div>

        {!bp ? (
          <div className="glass rounded-xl p-8 text-center text-slate-400">
            <p className="text-base animate-pulse">Assembling the evidence-based startup blueprint…</p>
          </div>
        ) : (
          <>
            {/* Header Banner */}
            <div className="glass rounded-2xl p-6 mb-8 border border-white/10 bg-gradient-to-br from-ink-900/90 to-ink-950/90">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-mono uppercase tracking-wider text-amber-400 bg-amber-500/10 px-2.5 py-0.5 rounded-full border border-amber-500/20">
                  CEO Final Synthesis
                </span>
                <SourceBadge source="CEO + Boardroom" />
              </div>
              <h1 className="text-3xl font-extrabold text-white tracking-tight">{bp.name}</h1>
              <p className="text-slate-200 mt-2 text-base leading-relaxed font-light">{bp.pitch}</p>
            </div>

            {/* Quick Navigation Jump Bar */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-3 mb-8 scroll-slim text-[11px] text-slate-300">
              <a href="#summary" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">1. Summary</a>
              <a href="#problem" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">2. Problem</a>
              <a href="#solution" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">3. Solution</a>
              <a href="#mvp" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">4. MVP</a>
              <a href="#tech" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">5. Technical</a>
              <a href="#gtm" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">6. GTM</a>
              <a href="#finance" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">7. Finance</a>
              <a href="#boardroom" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">8. Boardroom</a>
              <a href="#why" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">9. Why This Plan?</a>
              <a href="#risks" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">10. Risks</a>
              <a href="#assumptions" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">11. Assumptions</a>
              <a href="#roadmap" className="px-2.5 py-1 rounded bg-white/5 hover:bg-white/10 whitespace-nowrap">12. 90-Day Plan</a>
            </div>

            {/* 1. EXECUTIVE SUMMARY */}
            <Section id="summary" title="1. Executive Summary" source={bp.executiveSummarySection?.source || "CEO"}>
              <div className="grid sm:grid-cols-2 gap-3 mb-4">
                <div className="glass rounded-xl p-4">
                  <div className="text-xs text-slate-400 mb-1">Target Customer</div>
                  <div className="text-sm font-medium text-slate-100">{bp.targetCustomer}</div>
                </div>
                <div className="glass rounded-xl p-4">
                  <div className="text-xs text-slate-400 mb-1">Business Model</div>
                  <div className="text-sm font-medium text-slate-100">{bp.businessModel}</div>
                </div>
              </div>
              {bp.executiveSummarySection?.strategicObjectives && (
                <div className="glass rounded-xl p-4">
                  <div className="text-xs text-slate-400 mb-2">CEO Strategic Objectives</div>
                  <ul className="list-disc list-inside space-y-1 text-sm text-slate-200">
                    {bp.executiveSummarySection.strategicObjectives.map((obj, i) => (
                      <li key={i}>{obj}</li>
                    ))}
                  </ul>
                </div>
              )}
            </Section>

            {/* 2. PROBLEM */}
            <Section id="problem" title="2. Problem Statement & Pain Points" source={bp.problemSection?.source || "CEO + Marketing"}>
              <div className="glass rounded-xl p-4 mb-3">
                <div className="text-xs text-slate-400 mb-1">Problem Statement</div>
                <div className="text-sm text-slate-100 leading-relaxed">{bp.problem}</div>
              </div>
              <div className="grid sm:grid-cols-2 gap-3">
                <div className="glass rounded-xl p-4">
                  <div className="text-xs text-slate-400 mb-2">Customer Pain Points</div>
                  <ul className="list-disc list-inside space-y-1 text-xs text-slate-300">
                    {(bp.problemSection?.customerPainPoints || []).map((p, i) => (
                      <li key={i}>{p}</li>
                    ))}
                  </ul>
                </div>
                <div className="glass rounded-xl p-4">
                  <div className="text-xs text-slate-400 mb-2">Current Alternatives</div>
                  <ul className="list-disc list-inside space-y-1 text-xs text-slate-300">
                    {(bp.problemSection?.currentAlternatives || []).map((alt, i) => (
                      <li key={i}>{alt}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </Section>

            {/* 3. SOLUTION */}
            <Section id="solution" title="3. Solution & Product Workflow" source={bp.solutionSection?.source || "CEO + Development + Marketing"}>
              <div className="glass rounded-xl p-4 mb-3">
                <div className="text-xs text-slate-400 mb-1">Product Description</div>
                <div className="text-sm text-slate-100 leading-relaxed">{bp.solution}</div>
              </div>
              <div className="grid sm:grid-cols-2 gap-3">
                <div className="glass rounded-xl p-4">
                  <div className="text-xs text-slate-400 mb-2">Primary User Workflow</div>
                  <ol className="space-y-1.5 text-xs text-slate-300">
                    {(bp.solutionSection?.primaryUserWorkflow || []).map((step, i) => (
                      <li key={i} className="flex items-start gap-2">
                        <span className="text-sky-400 font-mono font-bold shrink-0">{i + 1}.</span>
                        <span>{step.replace(/^\d+\.\s*/, "")}</span>
                      </li>
                    ))}
                  </ol>
                </div>
                <div className="glass rounded-xl p-4">
                  <div className="text-xs text-slate-400 mb-2">Key Differentiators</div>
                  <ul className="list-disc list-inside space-y-1 text-xs text-slate-300">
                    {(bp.solutionSection?.keyDifferentiators || []).map((diff, i) => (
                      <li key={i}>{diff}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </Section>

            {/* 4. MVP & DEFERRED FEATURES */}
            <Section id="mvp" title="4. MVP Scope & Deferred Features" source={bp.mvpSection?.source || "Development + Boardroom + CEO"}>
              <div className="mb-4">
                <div className="text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-2 flex items-center justify-between">
                  <span>MVP Features (V1 Release)</span>
                  <span className="text-[10px] text-slate-500 font-mono">
                    {bp.mvpSection?.mvpFeatures.length || bp.mvp.length} included
                  </span>
                </div>
                <div className="space-y-2">
                  {(bp.mvpSection?.mvpFeatures || []).map((f) => (
                    <FeatureCard key={f.id} f={f} isMvp={true} />
                  ))}
                  {(!bp.mvpSection || bp.mvpSection.mvpFeatures.length === 0) &&
                    bp.mvp.map((f) => (
                      <div key={f.id} className="glass rounded-xl p-3 text-sm text-white">
                        {f.name} — {f.description}
                      </div>
                    ))}
                </div>
              </div>

              {(bp.mvpSection?.deferredFeatures?.length || 0) > 0 && (
                <div>
                  <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2 flex items-center justify-between">
                    <span>Deferred Features (V2 Release)</span>
                    <span className="text-[10px] text-slate-500 font-mono">
                      {bp.mvpSection?.deferredFeatures.length} deferred during boardroom debate
                    </span>
                  </div>
                  <div className="space-y-2">
                    {bp.mvpSection?.deferredFeatures.map((f) => (
                      <FeatureCard key={f.id} f={f} isMvp={false} />
                    ))}
                  </div>
                </div>
              )}
            </Section>

            {/* 5. TECHNICAL PLAN */}
            <Section id="tech" title="5. Technical Architecture & Engineering Plan" source={bp.technicalSection?.source || "Development"}>
              <div className="glass rounded-xl p-4 space-y-3 text-sm">
                <div className="grid sm:grid-cols-2 gap-3">
                  <div>
                    <span className="text-xs text-slate-400 block mb-0.5">Architecture</span>
                    <span className="text-slate-100 font-medium">{bp.technicalSection?.architecture || bp.technical.architecture}</span>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400 block mb-0.5">Development Timeline</span>
                    <span className="text-sky-300 font-mono font-semibold">{bp.technicalSection?.developmentTimeline || bp.technical.timeline}</span>
                  </div>
                </div>
                <div className="h-px bg-white/5" />
                <div className="grid sm:grid-cols-3 gap-2 text-xs">
                  <div className="bg-black/30 p-2.5 rounded-lg">
                    <span className="text-slate-400 block mb-1 font-mono">Frontend</span>
                    <span className="text-slate-200">{bp.technicalSection?.frontend || "React / TypeScript SPA"}</span>
                  </div>
                  <div className="bg-black/30 p-2.5 rounded-lg">
                    <span className="text-slate-400 block mb-1 font-mono">Backend</span>
                    <span className="text-slate-200">{bp.technicalSection?.backend || "Spring Boot REST API"}</span>
                  </div>
                  <div className="bg-black/30 p-2.5 rounded-lg">
                    <span className="text-slate-400 block mb-1 font-mono">Database & ML</span>
                    <span className="text-slate-200">{bp.technicalSection?.database || "PostgreSQL / H2"}</span>
                  </div>
                </div>
                {(bp.technicalSection?.technicalRisks?.length || 0) > 0 && (
                  <div className="mt-2">
                    <span className="text-xs text-amber-400 block mb-1 font-medium">Technical Risks</span>
                    <ul className="list-disc list-inside text-xs text-slate-300 space-y-0.5">
                      {bp.technicalSection?.technicalRisks.map((r, i) => (
                        <li key={i}>{r}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </Section>

            {/* 6. GO-TO-MARKET PLAN */}
            <Section id="gtm" title="6. Go-To-Market & Growth Strategy" source={bp.gtmSection?.source || "Marketing"}>
              <div className="glass rounded-xl p-4 space-y-3 text-sm">
                <div className="grid sm:grid-cols-2 gap-3">
                  <div>
                    <span className="text-xs text-slate-400 block mb-0.5">Positioning</span>
                    <span className="text-slate-100 font-medium">{bp.gtmSection?.positioning || bp.marketing.positioning}</span>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400 block mb-0.5">Pricing Model</span>
                    <span className="text-emerald-400 font-medium">{bp.gtmSection?.messaging || bp.marketing.pricingStrategy}</span>
                  </div>
                </div>
                <div className="h-px bg-white/5" />
                <div>
                  <span className="text-xs text-slate-400 block mb-1">Acquisition Channels</span>
                  <div className="flex flex-wrap gap-1.5">
                    {(bp.gtmSection?.acquisitionChannels || []).map((ch, i) => (
                      <span key={i} className="text-xs px-2 py-0.5 rounded bg-pink-500/10 text-pink-300 border border-pink-500/20">
                        {ch}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </Section>

            {/* 7. FINANCIAL PLAN */}
            <Section id="finance" title="7. Financial Plan & Runway Model" source={bp.financialSection?.source || "Finance"}>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mb-3">
                <Stat label="Starting Capital" v={money(bp.financialSection?.startingCapital || bp.budget.startingCapital)} />
                <Stat label="Total Upfront Cost" v={money(bp.financialSection?.totalUpfrontCost || bp.budget.totalUpfrontCost)} />
                <Stat label="Monthly Burn" v={money(bp.financialSection?.monthlyBurn || bp.budget.monthlyBurn)} />
                <Stat label="Runway" v={`${(bp.financialSection?.runwayMonths || bp.budget.runwayMonths).toFixed(1)} mo`} />
              </div>
              <div className="glass rounded-xl p-4 text-sm space-y-2">
                <p className="text-slate-200">{bp.revenueModel}</p>
                {bp.financialSection?.breakEvenConsiderations && (
                  <p className="text-xs text-slate-400 border-t border-white/5 pt-2">
                    <span className="font-semibold text-slate-300">Break-even target: </span>
                    {bp.financialSection.breakEvenConsiderations}
                  </p>
                )}
              </div>
            </Section>

            {/* 8. BOARDROOM DECISIONS */}
            <Section id="boardroom" title="8. Decisions from the Boardroom" source={bp.boardroomSection?.source || "Boardroom Debate + CEO Synthesis"}>
              {bp.boardroomSection?.disagreements && (
                <div className="glass rounded-xl p-4 mb-3 border border-purple-500/20 bg-purple-500/5">
                  <div className="text-xs font-semibold text-purple-300 uppercase tracking-wider mb-2">
                    ⚡ What Did the Agents Disagree About?
                  </div>
                  <ul className="list-disc list-inside space-y-1 text-xs text-slate-200">
                    {bp.boardroomSection.disagreements.map((dis, i) => (
                      <li key={i}>{dis}</li>
                    ))}
                  </ul>
                </div>
              )}

              {bp.boardroomSection?.debateChanges && (
                <div className="glass rounded-xl p-4 mb-4 border border-sky-500/20 bg-sky-500/5">
                  <div className="text-xs font-semibold text-sky-300 uppercase tracking-wider mb-2">
                    🔄 What Changed During the Debate?
                  </div>
                  <ul className="list-disc list-inside space-y-1 text-xs text-slate-200">
                    {bp.boardroomSection.debateChanges.map((chg, i) => (
                      <li key={i}>{chg}</li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="space-y-3">
                {(bp.boardroomSection?.decisions || []).map((d: BoardroomDecisionItem) => (
                  <div key={d.id} className="glass rounded-xl p-4 border border-white/10">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-bold text-white uppercase tracking-wider">
                        CEO Decision #{d.id}
                      </span>
                      <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-500/15 text-emerald-400 font-mono">
                        {d.finalStatus}
                      </span>
                    </div>
                    <div className="text-sm font-semibold text-amber-300 mb-1">{d.decision}</div>
                    <div className="text-xs text-slate-300 mb-2">{d.reason}</div>
                    {d.supportingArguments?.length > 0 && (
                      <div className="text-[11px] text-slate-400 bg-black/20 p-2 rounded">
                        <span className="text-emerald-400 font-semibold block mb-0.5">Accepted Arguments:</span>
                        {d.supportingArguments.join(" • ")}
                      </div>
                    )}
                  </div>
                ))}
                {(!bp.boardroomSection?.decisions || bp.boardroomSection.decisions.length === 0) &&
                  bp.decisions.map((d) => (
                    <div key={d.id} className="glass rounded-xl p-3 text-sm text-white">
                      {d.decision}
                    </div>
                  ))}
              </div>
            </Section>

            {/* 9. WHY THIS PLAN? */}
            <Section id="why" title="9. Why This Plan? (Reasoning Chain)" source={bp.whyThisPlanSection?.source || "Boardroom Debate + CEO Synthesis"}>
              <div className="glass rounded-xl p-5 border border-white/10">
                <p className="text-xs text-slate-300 mb-4">{bp.whyThisPlanSection?.summary}</p>
                <div className="space-y-3 relative before:absolute before:left-3.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-sky-500/20">
                  {(bp.whyThisPlanSection?.reasoningChain || []).map((step: ReasoningStep) => (
                    <div key={step.stepNumber} className="flex items-start gap-3 relative pl-1">
                      <div className="w-6 h-6 rounded-full bg-sky-500/20 border border-sky-400 text-sky-300 font-mono text-xs font-bold flex items-center justify-center shrink-0 z-10">
                        {step.stepNumber}
                      </div>
                      <div className="flex-1 bg-ink-900/60 rounded-lg p-3 border border-white/5">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-white">{step.title}</span>
                          <span className="text-[10px] font-mono text-slate-400">{step.department}</span>
                        </div>
                        <p className="text-xs text-slate-300 mt-1">{step.description}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </Section>

            {/* 10. RISKS */}
            <Section id="risks" title="10. Aggregated Startup Risks" source={bp.risksSection?.source || "CEO + Development + Marketing + Finance + Boardroom"}>
              <div className="glass rounded-xl p-4">
                <div className="space-y-2">
                  {(bp.risksSection?.aggregatedRisks || []).map((r, i) => (
                    <div key={i} className="flex items-center justify-between text-xs text-slate-200 bg-black/20 p-2.5 rounded-lg border border-white/5">
                      <span>⚠️ {r.description}</span>
                      <SourceBadge source={r.source} />
                    </div>
                  ))}
                  {(!bp.risksSection || bp.risksSection.aggregatedRisks.length === 0) &&
                    bp.risks.map((r, i) => <div key={i} className="text-xs text-slate-300">• {r}</div>)}
                </div>
              </div>
            </Section>

            {/* 11. ASSUMPTIONS */}
            <Section id="assumptions" title="11. Strategic Assumptions" source={bp.assumptionsSection?.source || "CEO + Development + Marketing + Finance"}>
              <div className="glass rounded-xl p-4">
                <div className="space-y-2">
                  {(bp.assumptionsSection?.aggregatedAssumptions || []).map((a, i) => (
                    <div key={i} className="flex items-start justify-between gap-3 text-xs bg-black/20 p-2.5 rounded-lg border border-white/5">
                      <div>
                        <span className="font-mono text-amber-400 font-bold mr-1.5">ASSUMPTION</span>
                        <span className="text-slate-200">{a.statement.replace(/^ASSUMPTION:\s*/i, "")}</span>
                      </div>
                      <SourceBadge source={a.source} />
                    </div>
                  ))}
                </div>
              </div>
            </Section>

            {/* 12. 90-DAY EXECUTION PLAN */}
            <Section id="roadmap" title="12. 90-Day Execution Roadmap" source={bp.executionPlanSection?.source || "CEO + Development + Marketing + Finance + Decision"}>
              <div className="space-y-3">
                {(bp.executionPlanSection?.stages || []).map((stage, i) => (
                  <div key={i} className="glass rounded-xl p-4 border border-white/10">
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-mono font-bold text-ceo px-2 py-0.5 rounded bg-amber-500/10 border border-amber-500/20">
                          {stage.stageName}
                        </span>
                        <span className="text-sm font-semibold text-white">{stage.focus}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        {stage.responsibleDepartments?.map((d, idx) => (
                          <SourceBadge key={idx} source={d} />
                        ))}
                      </div>
                    </div>
                    {stage.keyDeliverables?.length > 0 && (
                      <div className="mt-2.5 bg-black/20 p-2.5 rounded-lg">
                        <span className="text-[11px] font-semibold text-slate-400 block mb-1">Key Deliverables</span>
                        <ul className="list-disc list-inside text-xs text-slate-300 space-y-0.5">
                          {stage.keyDeliverables.map((del, dIdx) => (
                            <li key={dIdx}>{del}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Section>
          </>
        )}
      </div>
    </div>
  );
}

function Stat({ label, v }: { label: string; v: string }) {
  return (
    <div className="glass rounded-lg p-2.5">
      <div className="text-[10px] text-slate-400">{label}</div>
      <div className="text-sm font-bold text-white">{v}</div>
    </div>
  );
}
