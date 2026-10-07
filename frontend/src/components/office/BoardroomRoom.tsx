import type { Debate, SimulationPhase } from "../../types";

interface Props {
  inBoardroom: boolean;
  phase?: SimulationPhase;
  activeDebate?: Debate;
  onClick: () => void;
}

export default function BoardroomRoom({ inBoardroom, phase, activeDebate, onClick }: Props) {
  const lastMsg = activeDebate?.messages?.slice(-1)[0];
  const roundNum = lastMsg?.debateRound || 1;
  const roundType = lastMsg?.messageType || (phase === "DECISION" ? "SYNTHESIS" : "POSITION");

  return (
    <button
      onClick={onClick}
      className={`iso-room group relative text-left rounded-xl border-2 floor-boardroom transition-all duration-500 ${
        inBoardroom
          ? "border-ceo shadow-[0_0_60px_10px_rgba(245,179,1,0.5)] ring-2 ring-ceo animate-boardroom-glow"
          : "border-purple-500/40 hover:border-purple-400"
      }`}
      style={{ width: 260, height: 240 }}
    >
      {/* Architectural Floor Grid */}
      <div className="room-floor-base absolute inset-0 rounded-xl overflow-hidden opacity-30" />

      {/* Upright Content Plane (Facing Camera) */}
      <div className="iso-upright absolute inset-0 p-3 flex flex-col justify-between">
        {/* Room Header & Small Architectural Signage */}
        <div className="flex items-center justify-between border-b border-purple-500/30 pb-1">
          <div className="flex items-center gap-1.5">
            <span className="text-sm">🪑</span>
            <span className="text-xs font-extrabold uppercase tracking-widest text-purple-300 font-mono">
              BOARDROOM
            </span>
          </div>
          {inBoardroom ? (
            <span className="text-[9px] font-mono px-2 py-0.5 rounded bg-ceo/20 text-ceo border border-ceo/50 animate-pulse font-bold">
              IN SESSION
            </span>
          ) : (
            <span className="text-[9px] font-mono px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
              Standing By
            </span>
          )}
        </div>

        {/* Center: Presentation Screen & Conference Table */}
        <div className="relative flex-1 flex flex-col items-center justify-center my-1">
          {/* Wall Presentation Display */}
          <div className="bg-ink-950 border border-purple-400/50 rounded p-1.5 w-48 mb-2 text-center shadow-md">
            <div className="text-[8px] font-mono text-purple-300 flex items-center justify-between border-b border-purple-500/20 pb-0.5 mb-1">
              <span>🖥️ Boardroom Screen</span>
              <span className="text-ceo font-bold">
                {inBoardroom ? `ROUND ${roundNum}: ${roundType}` : "Ready"}
              </span>
            </div>
            <div className="text-[7.5px] text-slate-300 truncate font-mono">
              {inBoardroom && lastMsg
                ? `“${lastMsg.content}”`
                : "Autonomous boardroom debate for V1 scope & positioning."}
            </div>
          </div>

          {/* Oval Conference Table & Seated Department Representatives */}
          <div className="relative w-44 h-12 bg-gradient-to-r from-amber-950 via-amber-900 to-amber-950 border-2 border-amber-500/60 rounded-2xl flex items-center justify-around px-2 shadow-2xl">
            {/* CEO Seat */}
            <div
              className={`w-6 h-6 rounded-full border-2 border-amber-400 flex items-center justify-center text-xs shadow ${
                inBoardroom ? "bg-amber-500/30 text-amber-300 animate-bounce" : "bg-ink-900 text-slate-400"
              }`}
              title="CEO Representative"
            >
              👔
            </div>
            {/* Dev Seat */}
            <div
              className={`w-6 h-6 rounded-full border-2 border-sky-400 flex items-center justify-center text-xs shadow ${
                inBoardroom ? "bg-sky-500/30 text-sky-300 animate-bounce" : "bg-ink-900 text-slate-400"
              }`}
              title="Development Representative"
            >
              💻
            </div>
            {/* Marketing Seat */}
            <div
              className={`w-6 h-6 rounded-full border-2 border-pink-400 flex items-center justify-center text-xs shadow ${
                inBoardroom ? "bg-pink-500/30 text-pink-300 animate-bounce" : "bg-ink-900 text-slate-400"
              }`}
              title="Marketing Representative"
            >
              📣
            </div>
            {/* Finance Seat */}
            <div
              className={`w-6 h-6 rounded-full border-2 border-emerald-400 flex items-center justify-center text-xs shadow ${
                inBoardroom ? "bg-emerald-500/30 text-emerald-300 animate-bounce" : "bg-ink-900 text-slate-400"
              }`}
              title="Finance Representative"
            >
              📊
            </div>
          </div>
        </div>

        {/* Action / Click Hint */}
        <div className="text-center bg-purple-500/10 p-1 rounded border border-purple-500/20 text-[8.5px] font-mono text-purple-200">
          {inBoardroom ? "⚖️ Boardroom in Session — Click to view transcript" : "Click to view boardroom details"}
        </div>
      </div>
    </button>
  );
}
