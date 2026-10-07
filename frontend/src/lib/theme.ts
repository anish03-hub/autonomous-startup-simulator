import type { AgentState, AgentType, SimulationPhase, TaskPriority } from "../types";

export const DEPARTMENTS: AgentType[] = ["CEO", "DEVELOPMENT", "MARKETING", "FINANCE"];

interface DeptTheme {
  label: string;
  accent: string; // tailwind text/border color token
  hex: string;
  glow: string;
  emoji: string;
  room: string;
}

export const DEPT: Record<AgentType, DeptTheme> = {
  CEO: {
    label: "CEO Office",
    accent: "text-ceo",
    hex: "#f5b301",
    glow: "shadow-[0_0_40px_-8px_rgba(245,179,1,0.5)]",
    emoji: "👔",
    room: "from-amber-500/20 to-amber-900/10 border-ceo/40",
  },
  DEVELOPMENT: {
    label: "Development",
    accent: "text-dev",
    hex: "#38bdf8",
    glow: "shadow-[0_0_40px_-8px_rgba(56,189,248,0.5)]",
    emoji: "💻",
    room: "from-sky-500/20 to-sky-900/10 border-dev/40",
  },
  MARKETING: {
    label: "Marketing",
    accent: "text-marketing",
    hex: "#f472b6",
    glow: "shadow-[0_0_40px_-8px_rgba(244,114,182,0.5)]",
    emoji: "📣",
    room: "from-pink-500/20 to-pink-900/10 border-marketing/40",
  },
  FINANCE: {
    label: "Finance",
    accent: "text-finance",
    hex: "#34d399",
    glow: "shadow-[0_0_40px_-8px_rgba(52,211,153,0.5)]",
    emoji: "📊",
    room: "from-emerald-500/20 to-emerald-900/10 border-finance/40",
  },
};

export const STATE_LABEL: Record<AgentState, string> = {
  IDLE: "Idle",
  THINKING: "Thinking",
  WORKING: "Working",
  DISCUSSING: "In debate",
  WAITING: "Waiting",
  DECIDING: "Deciding",
  COMPLETED: "Done",
  WALKING: "Walking",
  BLOCKED: "Blocked",
  MEETING: "Meeting",
};

export const STATE_DOT: Record<AgentState, string> = {
  IDLE: "bg-slate-500",
  THINKING: "bg-indigo-400",
  WORKING: "bg-sky-400",
  DISCUSSING: "bg-pink-400",
  WAITING: "bg-amber-400",
  DECIDING: "bg-ceo",
  COMPLETED: "bg-emerald-400",
  WALKING: "bg-teal-400",
  BLOCKED: "bg-rose-500",
  MEETING: "bg-purple-400",
};

export const PHASES: SimulationPhase[] = [
  "IDEA",
  "ANALYSIS",
  "DEBATE",
  "DECISION",
  "PLAN",
  "COMPLETED",
];

export const PRIORITY_COLOR: Record<TaskPriority, string> = {
  LOW: "text-slate-400 border-slate-600",
  MEDIUM: "text-sky-300 border-sky-700",
  HIGH: "text-amber-300 border-amber-600",
  CRITICAL: "text-rose-300 border-rose-600",
};

export const money = (n: number) =>
  "$" + Math.round(n).toLocaleString("en-US");
