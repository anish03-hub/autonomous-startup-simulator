import type { AgentState, AgentType } from "../../types";

export interface CharacterProps {
  name: string;
  role: string;
  hairColor: string;
  shirtColor: string;
  skinTone: string;
  deptType: AgentType;
  state: AgentState;
  isLeader?: boolean;
}

export default function EmployeeCharacter({
  name,
  role,
  hairColor,
  shirtColor,
  skinTone,
  deptType,
  state,
}: CharacterProps) {
  const isWorking = state === "WORKING" || state === "THINKING" || state === "DECIDING";
  const isDebating = state === "DISCUSSING";

  return (
    <div className="relative flex flex-col items-center select-none">
      {/* Speech / Activity Bubble */}
      {(isWorking || isDebating) && (
        <div className="absolute -top-10 left-1/2 -translate-x-1/2 whitespace-nowrap bg-ink-950/95 border-2 border-white/30 px-2 py-0.5 rounded-md text-[9px] font-mono font-bold text-white shadow-2xl z-40 animate-bounce">
          {isDebating ? (
            <span className="text-pink-400">💬 Boardroom Debate</span>
          ) : (
            <span className="text-sky-300">
              {deptType === "DEVELOPMENT" && "⚡ Coding V1 MVP"}
              {deptType === "MARKETING" && "📣 GTM Strategy"}
              {deptType === "FINANCE" && "📊 Burn & Runway"}
              {deptType === "CEO" && "👔 CEO Synthesis"}
            </span>
          )}
        </div>
      )}

      {/* SEATED MINIATURE HUMAN FIGURE & CHAIR ASSEMBLY */}
      <div className="relative flex flex-col items-center">
        {/* OFFICE CHAIR BACKREST (Behind Torso) */}
        <div className="absolute -top-6 w-11 h-9 rounded-t-xl bg-slate-900 border-2 border-slate-700 shadow-inner z-0 flex items-center justify-center">
          <div className="w-7 h-6 rounded-t-lg bg-slate-800 border border-slate-700" />
        </div>

        {/* HUMAN CHARACTER BODY */}
        <div className="relative z-10 flex flex-col items-center">
          {/* HEAD WITH HAIR & Subtle Head Nod / Eye Movement */}
          <div className={`relative flex items-center justify-center ${isWorking ? "animate-head-nod" : ""}`}>
            {/* Hair Style */}
            <div
              className="absolute -top-1.5 w-7 h-4 rounded-t-full z-20 shadow-sm"
              style={{ backgroundColor: hairColor }}
            />
            {/* Face / Skin */}
            <div
              className="w-6 h-6 rounded-full border border-black/40 shadow-md flex flex-col items-center justify-center z-10"
              style={{ backgroundColor: skinTone }}
            >
              {/* Eyes Looking Toward Monitor */}
              <div className="flex gap-1 -mt-1">
                <span className="w-1.5 h-1.5 rounded-full bg-slate-900" />
                <span className="w-1.5 h-1.5 rounded-full bg-slate-900" />
              </div>
              {/* Cheerful Mouth */}
              <div className="w-2 h-0.5 bg-slate-700/60 rounded-full mt-0.5" />
            </div>
          </div>

          {/* TORSO & SHIRT (Leaning Slightly Forward Toward Desk) */}
          <div
            className="w-8 h-5 rounded-t-md border border-black/50 shadow-lg relative -mt-1 flex flex-col items-center justify-start z-10"
            style={{ backgroundColor: shirtColor }}
          >
            {/* Collar or Tie */}
            <div className="w-2 h-2.5 bg-white/40 rounded-b-sm" />

            {/* LEFT ARM & HAND (Reaching to Left Keyboard Side) */}
            <div
              className={`absolute -left-2.5 top-0.5 w-3.5 h-4 flex flex-col items-center z-20 ${
                isWorking ? "animate-left-typing" : ""
              }`}
            >
              {/* Sleeve */}
              <div className="w-2 h-2.5 rounded-full" style={{ backgroundColor: shirtColor }} />
              {/* Hand */}
              <div className="w-2 h-2 rounded-full border border-black/20" style={{ backgroundColor: skinTone }} />
            </div>

            {/* RIGHT ARM & HAND (Reaching to Right Keyboard Side) */}
            <div
              className={`absolute -right-2.5 top-0.5 w-3.5 h-4 flex flex-col items-center z-20 ${
                isWorking ? "animate-right-typing" : ""
              }`}
            >
              {/* Sleeve */}
              <div className="w-2 h-2.5 rounded-full" style={{ backgroundColor: shirtColor }} />
              {/* Hand */}
              <div className="w-2 h-2 rounded-full border border-black/20" style={{ backgroundColor: skinTone }} />
            </div>
          </div>

          {/* LEGS UNDER DESK */}
          <div className="flex gap-2 -mt-0.5 z-0">
            <span className="w-2.5 h-3.5 bg-slate-900 rounded-b border-x border-slate-800" />
            <span className="w-2.5 h-3.5 bg-slate-900 rounded-b border-x border-slate-800" />
          </div>

          {/* CHAIR SWIVEL WHEELS BASE */}
          <div className="w-10 h-2 bg-slate-950 border border-slate-800 rounded-full shadow-lg -mt-1 flex justify-between px-1.5 items-center">
            <span className="w-1.5 h-1.5 rounded-full bg-slate-700" />
            <span className="w-1.5 h-1.5 rounded-full bg-slate-700" />
            <span className="w-1.5 h-1.5 rounded-full bg-slate-700" />
          </div>
        </div>
      </div>

      {/* Employee Name & Role Label */}
      <div className="mt-1 text-center leading-none">
        <div className="text-[11px] font-extrabold text-white tracking-tight truncate max-w-[90px] drop-shadow">
          {name}
        </div>
        <div className="text-[9px] font-mono text-slate-300 truncate max-w-[90px]">
          {role}
        </div>
      </div>
    </div>
  );
}
