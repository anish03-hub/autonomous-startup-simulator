import type { AgentState, AgentType } from "../../types";
import ComputerMonitor from "./ComputerMonitor";
import EmployeeCharacter from "./EmployeeCharacter";

export interface WorkstationEmployeeProps {
  id: string;
  name: string;
  role: string;
  hairColor: string;
  shirtColor: string;
  skinTone: string;
  deptType: AgentType;
  state: AgentState;
  speechText?: string;
  isLeader?: boolean;
}

interface Props {
  employee: WorkstationEmployeeProps;
  isExecutive?: boolean;
}

export default function Workstation({ employee, isExecutive }: Props) {
  const isWorking =
    employee.state === "WORKING" ||
    employee.state === "THINKING" ||
    employee.state === "DECIDING";

  return (
    <div className="relative flex flex-col items-center group cursor-pointer z-10 transition-transform hover:scale-105">
      {/* 2.5D ISOMETRIC PHYSICAL WORKSTATION UNIT */}
      <div className="relative flex flex-col items-center">
        {/* 1. COMPUTER MONITOR */}
        <ComputerMonitor deptType={employee.deptType} isExecutive={isExecutive} />

        {/* 2. PHYSICAL DESK SURFACE WITH REAL DEPTH */}
        <div
          className={`relative z-10 ${
            isExecutive
              ? "w-32 h-7 bg-gradient-to-r from-amber-950 via-amber-900 to-amber-950 border-amber-600/70"
              : "w-28 h-6 bg-gradient-to-r from-slate-900 via-slate-850 to-slate-900 border-slate-700"
          } border-2 rounded-md shadow-2xl flex items-center justify-between px-2.5 -mt-1`}
        >
          {/* Keyboard with Animated Keycap Highlights */}
          <div className="w-12 h-2.5 bg-slate-950 rounded border border-slate-600 flex items-center justify-around px-1 shadow-inner">
            <span className={`w-1.5 h-1 rounded-sm ${isWorking ? "bg-amber-300 animate-key-glow" : "bg-white/40"}`} />
            <span className={`w-1.5 h-1 rounded-sm ${isWorking ? "bg-sky-300 animate-key-glow" : "bg-white/40"}`} />
            <span className={`w-1.5 h-1 rounded-sm ${isWorking ? "bg-emerald-300 animate-key-glow" : "bg-white/40"}`} />
          </div>

          {/* Mouse & Pad */}
          <div className="w-2.5 h-3 bg-slate-800 rounded-full border border-slate-600 shadow-sm" />
        </div>

        {/* 3. SEATED MINIATURE HUMAN EMPLOYEE IN OFFICE CHAIR */}
        <div className="relative -mt-2 z-30">
          <EmployeeCharacter
            name={employee.name}
            role={employee.role}
            hairColor={employee.hairColor}
            shirtColor={employee.shirtColor}
            skinTone={employee.skinTone}
            deptType={employee.deptType}
            state={employee.state}
            isLeader={employee.isLeader}
          />
        </div>
      </div>
    </div>
  );
}
