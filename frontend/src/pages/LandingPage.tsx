import { useState } from "react";
import { useSim } from "../store/simulationStore";

const EXAMPLES = [
  "An AI-powered app that scans skincare products and recommends a routine",
  "A marketplace connecting local farmers directly with restaurants",
  "A tool that turns meeting recordings into action items and follow-ups",
];

export default function LandingPage({ onReady }: { onReady: () => void }) {
  const create = useSim((s) => s.create);
  const startSimulation = useSim((s) => s.startSimulation);
  const loading = useSim((s) => s.loading);
  const error = useSim((s) => s.error);

  const [idea, setIdea] = useState("");
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (idea.trim().length < 8 || busy) return;
    setBusy(true);
    try {
      await create(idea.trim(), name.trim() || undefined);
      await startSimulation(1.0);
      onReady();
    } catch {
      setBusy(false);
    }
  };

  const working = busy || loading;

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-6 relative overflow-hidden">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,_rgba(56,189,248,0.12),transparent_55%)]" />
      <div className="relative w-full max-w-2xl">
        <div className="text-center mb-8 animate-fade-in-up">
          <div className="inline-flex items-center gap-2 text-xs font-mono uppercase tracking-widest text-sky-400/80 mb-3">
            <span className="h-1.5 w-1.5 rounded-full bg-sky-400 animate-pulse" />
            Autonomous Virtual Startup
          </div>
          <h1 className="text-4xl sm:text-5xl font-extrabold text-white leading-tight">
            You're the founder.
            <br />
            <span className="bg-gradient-to-r from-sky-400 via-pink-400 to-ceo bg-clip-text text-transparent">
              Your AI team builds the company.
            </span>
          </h1>
          <p className="mt-4 text-slate-400 max-w-xl mx-auto">
            Drop in an idea. Watch your CEO, Development, Marketing, and Finance
            agents analyze it, debate the trade-offs, and converge on a real
            startup blueprint — live, in the office.
          </p>
        </div>

        <form
          onSubmit={submit}
          className="glass rounded-2xl p-5 sm:p-6 shadow-2xl animate-fade-in-up"
        >
          <label className="block text-sm font-medium text-slate-300 mb-2">
            What's your startup idea?
          </label>
          <textarea
            value={idea}
            onChange={(e) => setIdea(e.target.value)}
            placeholder="Describe your idea in a sentence or two…"
            rows={3}
            className="w-full resize-none rounded-xl bg-ink-900 border border-white/10 px-4 py-3 text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-sky-500/50"
          />

          <div className="mt-3 flex flex-wrap gap-2">
            {EXAMPLES.map((ex) => (
              <button
                key={ex}
                type="button"
                onClick={() => setIdea(ex)}
                className="text-xs text-slate-400 hover:text-white border border-white/10 hover:border-sky-500/50 rounded-full px-3 py-1 transition"
              >
                {ex.length > 42 ? ex.slice(0, 42) + "…" : ex}
              </button>
            ))}
          </div>

          <div className="mt-4 flex flex-col sm:flex-row gap-3">
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Company name (optional)"
              className="flex-1 rounded-xl bg-ink-900 border border-white/10 px-4 py-3 text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-sky-500/50"
            />
            <button
              type="submit"
              disabled={working || idea.trim().length < 8}
              className="rounded-xl px-6 py-3 font-semibold bg-gradient-to-r from-sky-500 to-indigo-500 text-white shadow-lg shadow-sky-900/40 disabled:opacity-40 disabled:cursor-not-allowed hover:brightness-110 transition"
            >
              {working ? "Founding…" : "Found the startup →"}
            </button>
          </div>

          {error && (
            <p className="mt-3 text-sm text-rose-400">Couldn't start: {error}</p>
          )}
        </form>

        <p className="text-center text-xs text-slate-600 mt-6">
          Phase 1 · mock AI reasoning · backend owns all state
        </p>
      </div>
    </div>
  );
}
