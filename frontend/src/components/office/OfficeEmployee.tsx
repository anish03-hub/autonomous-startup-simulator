import type { AgentState, AgentType } from "../../types";
import { STATE_DOT, STATE_LABEL } from "../../lib/theme";

export interface EmployeeInfo {
  id: string;
  name: string;
  role: string;
  avatarGlyph: string;
  deptType: AgentType;
  state: AgentState;
  activity?: string;
  isMainLeader?: boolean;
}

interface Props {
  employee: EmployeeInfo;
  isWorking?: boolean;
  isDebating?: boolean;
}

export default function OfficeEmployee({ employee, isWorking, isDebating }: Props) {
  const active =
    employee.state === "WORKING" ||
    employee.state === "THINKING" ||
    employee.state === "DECIDING" ||
    isWorking;

  const inBoardroom = employee.state === "DISCUSSING" || isDebating;

  // Department-specific clothing/accent colors
  const deptAccentColor = {
    CEO: "from-amber-500 to-amber-700 border-amber-400 text-amber-300",
    DEVELOPMENT: "from-sky-500 to-blue-700 border-sky-400 text-sky-300",
    MARKETING: "from-pink-500 to-rose-700 border-pink-400 text-pink-300",
    FINANCE: "from-emerald-500 to-teal-700 border-emerald-400 text-emerald-300",
  }[employee.deptType];

  return (
    <div className="relative flex flex-col items-center group cursor-pointer z-10 transition-transform hover:scale-110">
      {/* Speech / Activity Bubble when active */}
      {(active || inBoardroom || employee.activity) && (
        <div className="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap bg-ink-900/90 border border-white/20 px-2 py-0.5 rounded-full text-[9px] text-slate-100 font-mono shadow-lg flex items-center gap-1 animate-bounce pointer-events-none">
          {inBoardroom ? (
            <span className="text-pink-400 font-bold">💬 Debating</span>
          ) : active ? (
            <span className="text-sky-300">⚡ Working</span>
          ) : (
            <span className="text-slate-300">{employee.role}</span>
          )}
        </div>
      )}

      {/* Main Avatar Character Body */}
      <div className="relative flex flex-col items-center">
        {/* Status Pulse Ring */}
        {active && (
          <span
            className={`absolute -inset-1 rounded-full ${STATE_DOT[employee.state] || "bg-sky-400"} opacity-40 animate-ping`}
          />
        )}

        {/* Character Head & Torso */}
        <div
          className={`relative flex items-center justify-center h-9 w-9 rounded-full bg-gradient-to-b ${deptAccentColor} border-2 shadow-md text-sm ${
            active ? "animate-avatar-bob" : ""
          }`}
          title={`${employee.name} (${employee.role}) — ${STATE_LABEL[employee.state]}`}
        >
          <span>{employee.avatarGlyph}</span>
          
          {/* Status Indicator Dot */}
          <span
            className={`absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-ink-950 ${
              STATE_DOT[employee.state] || "bg-slate-500"
            }`}
          />
        </div>

        {/* Hands / Typing Hands for seated working pose */}
        {active && (
          <div className="flex gap-1 -mt-1 animate-typing z-20">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-200/80" />
            <span className="w-1.5 h-1.5 rounded-full bg-amber-200/80" />
          </div>
        )}
      </div>

      {/* Name Label */}
      <div className="mt-1 text-center leading-none">
        <div className="text-[10px] font-bold text-white tracking-tight truncate max-w-[70px] drop-shadow">
          {employee.name}
        </div>
        <div className="text-[8px] font-mono text-slate-400 truncate max-w-[70px]">
          {employee.role}
        </div>
      </div>
    </div>
  );
}
