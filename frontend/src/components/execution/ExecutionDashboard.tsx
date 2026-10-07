import { useState } from "react";
import { useSim } from "../../store/simulationStore";
import type { AgentType, ExecutionTask, TaskPriority } from "../../types";

export default function ExecutionDashboard() {
  const executionState = useSim((s) => s.executionState);
  const startExecution = useSim((s) => s.startExecution);
  const pauseExecution = useSim((s) => s.pauseExecution);
  const resumeExecution = useSim((s) => s.resumeExecution);
  const stepExecution = useSim((s) => s.stepExecution);
  const resolveTaskBlocker = useSim((s) => s.resolveTaskBlocker);

  const [activeTab, setActiveTab] = useState<"ACTIVE" | "COMPLETED" | "BLOCKED" | "ALL">("ACTIVE");

  if (!executionState) {
    return (
      <div className="p-6 bg-slate-900/90 backdrop-blur-md rounded-2xl border border-slate-800 text-center select-none shadow-2xl">
        <h3 className="text-xl font-bold text-white mb-2">🚀 Phase 2F: Autonomous Startup Execution</h3>
        <p className="text-slate-400 text-sm mb-6 max-w-lg mx-auto">
          Convert the completed startup blueprint into an executable task graph. Watch autonomous department agents execute roadmap tasks in real-time.
        </p>
        <button
          onClick={() => void startExecution()}
          className="px-6 py-3 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-bold text-sm shadow-lg shadow-emerald-500/25 transition transform hover:scale-105"
        >
          ▶️ Start Autonomous Execution
        </button>
      </div>
    );
  }

  const {
    day,
    week,
    isRunning,
    isPaused,
    isCompleted,
    overallProgress,
    mvpProgress,
    technicalProgress,
    marketReadiness,
    financialHealth,
    budgetRemaining,
    runwayMonths,
    activeBlockerCount,
    departmentProgress,
    tasks = [],
  } = executionState;

  const activeTasks = tasks.filter((t) => t.status === "IN_PROGRESS" || t.status === "PENDING");
  const completedTasks = tasks.filter((t) => t.status === "COMPLETED");
  const blockedTasks = tasks.filter((t) => t.status === "BLOCKED");

  const filteredTasks =
    activeTab === "ACTIVE"
      ? activeTasks
      : activeTab === "COMPLETED"
      ? completedTasks
      : activeTab === "BLOCKED"
      ? blockedTasks
      : tasks;

  const getPriorityBadge = (p: TaskPriority) => {
    switch (p) {
      case "HIGH":
      case "CRITICAL":
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30">HIGH</span>;
      case "MEDIUM":
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-cyan-500/20 text-cyan-400 border border-cyan-500/30">MED</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-700 text-slate-300">LOW</span>;
    }
  };

  const getDeptColor = (dept: AgentType) => {
    switch (dept) {
      case "CEO":
        return "text-amber-400 border-amber-500/40 bg-amber-500/10";
      case "DEVELOPMENT":
        return "text-cyan-400 border-cyan-500/40 bg-cyan-500/10";
      case "MARKETING":
        return "text-pink-400 border-pink-500/40 bg-pink-500/10";
      case "FINANCE":
        return "text-emerald-400 border-emerald-500/40 bg-emerald-500/10";
    }
  };

  return (
    <div className="space-y-6 select-none">
      {/* Top Header & Simulation Controls */}
      <div className="flex flex-wrap items-center justify-between gap-4 p-5 bg-slate-900/90 backdrop-blur-md rounded-2xl border border-slate-800 shadow-xl">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-800 border border-slate-700">
            <span className="text-xl">📅</span>
            <div>
              <div className="text-xs text-slate-400 uppercase tracking-wider font-bold">Simulation Time</div>
              <div className="text-sm font-extrabold text-white">Day {day} <span className="text-slate-400 font-normal">(Week {week})</span></div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {isCompleted ? (
              <span className="px-3 py-1.5 rounded-xl bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 text-xs font-bold flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                Execution Completed
              </span>
            ) : isPaused ? (
              <span className="px-3 py-1.5 rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/40 text-xs font-bold flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-amber-400" />
                Paused
              </span>
            ) : isRunning ? (
              <span className="px-3 py-1.5 rounded-xl bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 text-xs font-bold flex items-center gap-1.5 animate-pulse">
                <span className="w-2 h-2 rounded-full bg-cyan-400" />
                Active Execution
              </span>
            ) : (
              <span className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-300 border border-slate-700 text-xs font-bold">
                Ready
              </span>
            )}

            {activeBlockerCount > 0 && (
              <span className="px-3 py-1.5 rounded-xl bg-rose-500/20 text-rose-300 border border-rose-500/40 text-xs font-bold flex items-center gap-1.5 animate-bounce">
                ⚠️ {activeBlockerCount} Blocker{activeBlockerCount > 1 ? "s" : ""}
              </span>
            )}
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2">
          {!isRunning && !isPaused && !isCompleted && (
            <button
              onClick={() => void startExecution()}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-md transition"
            >
              ▶️ Start Execution
            </button>
          )}

          {isRunning && (
            <button
              onClick={() => void pauseExecution()}
              className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-bold text-xs shadow-md transition"
            >
              ⏸️ Pause
            </button>
          )}

          {isPaused && (
            <button
              onClick={() => void resumeExecution()}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-md transition"
            >
              ▶️ Resume
            </button>
          )}

          <button
            onClick={() => void stepExecution()}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 hover:text-white font-bold text-xs shadow-md transition flex items-center gap-1.5"
          >
            ⏩ Advance +1 Day
          </button>
        </div>
      </div>

      {/* Primary Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="p-4 bg-slate-900/90 backdrop-blur-md rounded-2xl border border-slate-800 shadow-xl">
          <div className="flex items-center justify-between text-xs text-slate-400 font-bold mb-2">
            <span>OVERALL PROGRESS</span>
            <span className="text-emerald-400 font-mono text-sm">{overallProgress}%</span>
          </div>
          <div className="w-full h-2.5 bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-emerald-500 to-teal-400 transition-all duration-500" style={{ width: `${overallProgress}%` }} />
          </div>
          <div className="mt-2 text-[11px] text-slate-400">Roadmap Milestone Delivery</div>
        </div>

        <div className="p-4 bg-slate-900/90 backdrop-blur-md rounded-2xl border border-slate-800 shadow-xl">
          <div className="flex items-center justify-between text-xs text-slate-400 font-bold mb-2">
            <span>BUDGET REMAINING</span>
            <span className="text-cyan-400 font-mono text-sm">${Math.round(budgetRemaining).toLocaleString()}</span>
          </div>
          <div className="w-full h-2.5 bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-cyan-500 to-blue-500 transition-all duration-500" style={{ width: `${financialHealth}%` }} />
          </div>
          <div className="mt-2 text-[11px] text-slate-400">Financial Reserve Health</div>
        </div>

        <div className="p-4 bg-slate-900/90 backdrop-blur-md rounded-2xl border border-slate-800 shadow-xl">
          <div className="flex items-center justify-between text-xs text-slate-400 font-bold mb-2">
            <span>RUNWAY</span>
            <span className="text-amber-400 font-mono text-sm">{runwayMonths} Months</span>
          </div>
          <div className="w-full h-2.5 bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-amber-500 transition-all duration-500" style={{ width: `${Math.min(100, (runwayMonths / 18) * 100)}%` }} />
          </div>
          <div className="mt-2 text-[11px] text-slate-400">Operational Cash Runway</div>
        </div>

        <div className="p-4 bg-slate-900/90 backdrop-blur-md rounded-2xl border border-slate-800 shadow-xl">
          <div className="flex items-center justify-between text-xs text-slate-400 font-bold mb-2">
            <span>MVP READINESS</span>
            <span className="text-pink-400 font-mono text-sm">{mvpProgress}%</span>
          </div>
          <div className="w-full h-2.5 bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-pink-500 to-purple-500 transition-all duration-500" style={{ width: `${mvpProgress}%` }} />
          </div>
          <div className="mt-2 flex items-center justify-between text-[10px] text-slate-400">
            <span>Tech: {technicalProgress}%</span>
            <span>Market: {marketReadiness}%</span>
          </div>
        </div>
      </div>

      {/* Department Progress Bars */}
      <div className="p-5 bg-slate-900/90 backdrop-blur-md rounded-2xl border border-slate-800 shadow-xl">
        <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4">Department Execution Status</h4>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {Object.entries(departmentProgress || {}).map(([dept, prog]) => (
            <div key={dept} className="p-3 bg-slate-950/60 rounded-xl border border-slate-800/80">
              <div className="flex justify-between items-center text-xs font-bold mb-1.5">
                <span className="text-slate-300">{dept}</span>
                <span className="font-mono text-slate-400">{Math.round(prog)}%</span>
              </div>
              <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                <div
                  className={`h-full transition-all duration-500 ${
                    dept === "DEVELOPMENT" ? "bg-cyan-500" : dept === "MARKETING" ? "bg-pink-500" : dept === "FINANCE" ? "bg-emerald-500" : "bg-amber-500"
                  }`}
                  style={{ width: `${prog}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Execution Task Graph & Tabs */}
      <div className="p-5 bg-slate-900/90 backdrop-blur-md rounded-2xl border border-slate-800 shadow-xl space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 pb-3">
          <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Executable Task Graph</h4>
          <div className="flex items-center gap-1.5 bg-slate-950 p-1 rounded-xl border border-slate-800 text-xs">
            <button
              onClick={() => setActiveTab("ACTIVE")}
              className={`px-3 py-1 rounded-lg font-semibold transition ${activeTab === "ACTIVE" ? "bg-slate-800 text-white" : "text-slate-400 hover:text-slate-200"}`}
            >
              Active ({activeTasks.length})
            </button>
            <button
              onClick={() => setActiveTab("BLOCKED")}
              className={`px-3 py-1 rounded-lg font-semibold transition ${activeTab === "BLOCKED" ? "bg-rose-500/20 text-rose-300" : "text-slate-400 hover:text-slate-200"}`}
            >
              Blocked ({blockedTasks.length})
            </button>
            <button
              onClick={() => setActiveTab("COMPLETED")}
              className={`px-3 py-1 rounded-lg font-semibold transition ${activeTab === "COMPLETED" ? "bg-emerald-500/20 text-emerald-300" : "text-slate-400 hover:text-slate-200"}`}
            >
              Completed ({completedTasks.length})
            </button>
            <button
              onClick={() => setActiveTab("ALL")}
              className={`px-3 py-1 rounded-lg font-semibold transition ${activeTab === "ALL" ? "bg-slate-800 text-white" : "text-slate-400 hover:text-slate-200"}`}
            >
              All ({tasks.length})
            </button>
          </div>
        </div>

        {/* Task Cards List */}
        <div className="space-y-3 max-h-[420px] overflow-y-auto pr-1">
          {filteredTasks.length === 0 ? (
            <div className="text-center py-8 text-slate-500 text-xs">No tasks found in this section.</div>
          ) : (
            filteredTasks.map((task: ExecutionTask) => (
              <div
                key={task.id}
                className="p-4 bg-slate-950/70 rounded-xl border border-slate-800 hover:border-slate-700 transition flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1.5 max-w-xl">
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${getDeptColor(task.department)}`}>
                      {task.department}
                    </span>
                    {getPriorityBadge(task.priority)}
                    <span className="text-xs font-semibold text-slate-400">Assigned: {task.assignedAgent}</span>
                  </div>
                  <h5 className="text-sm font-bold text-white">{task.title}</h5>
                  <p className="text-xs text-slate-400 line-clamp-2">{task.description}</p>
                  {task.blockerReason && (
                    <div className="mt-2 p-2 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs font-medium">
                      ⚠️ {task.blockerReason}
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-4 min-w-[200px] justify-between md:justify-end">
                  <div className="text-right space-y-1 w-28">
                    <div className="text-xs font-bold text-slate-300">{Math.round(task.progress)}%</div>
                    <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className={`h-full transition-all duration-300 ${
                          task.status === "COMPLETED"
                            ? "bg-emerald-500"
                            : task.status === "BLOCKED"
                            ? "bg-rose-500"
                            : "bg-cyan-500"
                        }`}
                        style={{ width: `${task.progress}%` }}
                      />
                    </div>
                    <div className="text-[10px] text-slate-500 font-mono">{task.estimatedDays} est. days</div>
                  </div>

                  {task.status === "BLOCKED" && (
                    <button
                      onClick={() => void resolveTaskBlocker(task.id)}
                      className="px-3 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs shadow transition whitespace-nowrap"
                    >
                      Resolve Blocker
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
