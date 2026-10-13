import { useRef, useMemo, useEffect, memo } from "react";
import * as THREE from "three";
import { useSim } from "../../store/simulationStore";
import type { AgentType, Plans, Startup, Agent, ExecutionState, Debate } from "../../types";

interface Monitor3DProps {
  type: AgentType | "BOARDROOM";
  position?: [number, number, number];
  rotation?: [number, number, number];
  scale?: number;
  activeSpeaker?: string | null;
  currentRound?: number | null;
}

type ScreenState =
  | "IDLE"
  | "WORKING"
  | "COMMUNICATING"
  | "COMPLETED"
  | "BLOCKED"
  | "FAILED";

interface ScreenModel {
  status: ScreenState;
  task: string | null;
  progress: number | null; // 0..100
  blocker: string | null;
  findings: { label: string; value: string }[];
}

// Per-department screen palette (header bar bg, accent text, icon, title).
const SCREEN: Record<AgentType, { bar: string; accent: string; icon: string; title: string }> = {
  CEO: { bar: "#422006", accent: "#fbbf24", icon: "🎯", title: "CEO · STRATEGY" },
  DEVELOPMENT: { bar: "#082f3a", accent: "#38bdf8", icon: "⚡", title: "DEV · BUILD" },
  MARKETING: { bar: "#3b0a2e", accent: "#f472b6", icon: "📈", title: "MKT · GROWTH" },
  FINANCE: { bar: "#052e24", accent: "#34d399", icon: "💵", title: "FIN · RUNWAY" },
};

const STATUS_STYLE: Record<ScreenState, { label: string; color: string }> = {
  IDLE: { label: "IDLE", color: "#64748b" },
  WORKING: { label: "● WORKING", color: "#38bdf8" },
  COMMUNICATING: { label: "● COMMUNICATING", color: "#a78bfa" },
  COMPLETED: { label: "✔ COMPLETED", color: "#34d399" },
  BLOCKED: { label: "⚠ BLOCKED", color: "#f59e0b" },
  FAILED: { label: "✕ FAILED", color: "#f87171" },
};

function clean(s: string | null | undefined): string | null {
  if (!s) return null;
  const t = s.trim();
  return t.length ? t : null;
}

function money(n: number | null | undefined): string {
  if (n == null || !isFinite(n)) return "—";
  if (Math.abs(n) >= 1000) return "$" + Math.round(n / 1000) + "k";
  return "$" + Math.round(n);
}

interface StoreSlices {
  agents: Agent[];
  executionState: ExecutionState | null;
  plans: Plans | null;
  startup: Startup | null;
  debates: Debate[];
}

// Derive a department's live screen model purely from genuine, persisted store
// data (agents, execution tasks, plans, startup analysis). No fabricated or
// random content; each department reads only its own slice, so one agent's work
// never appears on another's monitor.
function buildModel(type: AgentType, s: StoreSlices): ScreenModel {
  const { agents, executionState, plans, startup, debates } = s;
  const agent = agents.find((a) => a.type === type);
  const agState = agent?.state ?? "IDLE";

  const deptTask = executionState?.tasks?.find(
    (t) => t.department === type && (t.status === "IN_PROGRESS" || t.status === "BLOCKED")
  );
  const deptProgress =
    executionState?.departmentProgress?.[type] ??
    (deptTask ? deptTask.progress : null);

  // Analysis-failure flags are genuine FAILED signals.
  const failed =
    (type === "CEO" && !!startup?.ceoAnalysisFailed) ||
    (type === "DEVELOPMENT" && !!plans?.technical?.analysisFailed) ||
    (type === "MARKETING" && !!plans?.marketing?.analysisFailed) ||
    (type === "FINANCE" && !!plans?.budget?.analysisFailed);

  let status: ScreenState;
  if (failed) status = "FAILED";
  else if (deptTask?.status === "BLOCKED" || agState === "BLOCKED") status = "BLOCKED";
  else if (agState === "DISCUSSING" || agState === "MEETING") status = "COMMUNICATING";
  else if (deptTask || agState === "WORKING" || agState === "THINKING" || agState === "DECIDING")
    status = "WORKING";
  else if (agState === "COMPLETED") status = "COMPLETED";
  else status = "IDLE";

  const task = clean(deptTask?.title) ?? clean(agent?.currentActivity);
  const progress = deptTask ? deptTask.progress : deptProgress;

  const findings: { label: string; value: string }[] = [];
  const push = (label: string, value: string | null) => {
    if (value) findings.push({ label, value });
  };

  if (type === "CEO") {
    const decision = debates[debates.length - 1]?.decision?.decision;
    push("Objective", clean(startup?.strategicObjectives?.[0]) ?? clean(startup?.mvpDirection));
    push("Decision", clean(decision));
    push("Summary", clean(startup?.executiveSummary));
  } else if (type === "DEVELOPMENT") {
    push("Stack", clean(plans?.technical?.techStack));
    push("Architecture", clean(plans?.technical?.architecture));
    push("Timeline", clean(plans?.technical?.timeline));
  } else if (type === "MARKETING") {
    push("Positioning", clean(plans?.marketing?.positioning));
    push("Channels", clean(plans?.marketing?.channels));
    push("Insight", clean(plans?.marketing?.competitorAnalysis));
  } else if (type === "FINANCE") {
    const runway = executionState?.runwayMonths ?? plans?.budget?.runwayMonths;
    if (runway != null && isFinite(runway)) push("Runway", runway.toFixed(1) + " mo");
    if (executionState?.budgetRemaining != null) push("Budget", money(executionState.budgetRemaining) + " left");
    if (plans?.budget?.monthlyBurn) push("Burn", money(plans.budget.monthlyBurn) + "/mo");
    push("Break-even", clean(plans?.budget?.breakEvenAssumption));
  }

  return { status, task, progress: progress ?? null, blocker: clean(deptTask?.blockerReason), findings };
}

// Wrap `text` to `maxWidth`, returning at most `maxLines` lines (last line
// ellipsised if it overflows). Keeps canvas text inside the monitor bezel.
function wrapLines(ctx: CanvasRenderingContext2D, text: string, maxWidth: number, maxLines: number): string[] {
  const words = text.split(/\s+/);
  const lines: string[] = [];
  let line = "";
  for (const w of words) {
    const trial = line ? line + " " + w : w;
    if (ctx.measureText(trial).width > maxWidth && line) {
      lines.push(line);
      line = w;
      if (lines.length === maxLines) break;
    } else {
      line = trial;
    }
  }
  if (lines.length < maxLines && line) lines.push(line);
  if (lines.length === maxLines) {
    let last = lines[maxLines - 1];
    while (last.length > 1 && ctx.measureText(last + "…").width > maxWidth) last = last.slice(0, -1);
    lines[maxLines - 1] = last + "…";
  }
  return lines;
}

function drawDepartmentScreen(ctx: CanvasRenderingContext2D, type: AgentType, model: ScreenModel) {
  const W = 512;
  const H = 320;
  const theme = SCREEN[type];
  const st = STATUS_STYLE[model.status];

  // Background
  ctx.fillStyle = "#0a0e17";
  ctx.fillRect(0, 0, W, H);

  // Header bar
  ctx.fillStyle = theme.bar;
  ctx.fillRect(0, 0, W, 40);
  ctx.fillStyle = theme.accent;
  ctx.font = "bold 18px sans-serif";
  ctx.textBaseline = "alphabetic";
  ctx.fillText(`${theme.icon} ${theme.title}`, 16, 27);

  // Status badge (right-aligned)
  ctx.font = "bold 13px monospace";
  ctx.fillStyle = st.color;
  const badgeW = ctx.measureText(st.label).width;
  ctx.fillText(st.label, W - badgeW - 16, 26);

  // Current task
  let y = 68;
  ctx.font = "13px sans-serif";
  ctx.fillStyle = "#64748b";
  ctx.fillText("CURRENT TASK", 16, y);
  y += 22;
  ctx.font = "bold 16px sans-serif";
  ctx.fillStyle = "#e2e8f0";
  const taskText = model.task ?? (model.status === "IDLE" ? "Standby — awaiting activity" : "—");
  wrapLines(ctx, taskText, W - 32, 2).forEach((ln) => {
    ctx.fillText(ln, 16, y);
    y += 22;
  });

  // Blocker (if any) or progress bar
  if (model.blocker) {
    y += 2;
    ctx.font = "13px sans-serif";
    ctx.fillStyle = "#fbbf24";
    wrapLines(ctx, "⚠ " + model.blocker, W - 32, 2).forEach((ln) => {
      ctx.fillText(ln, 16, y);
      y += 18;
    });
  } else if (model.progress != null) {
    y += 4;
    ctx.fillStyle = "#1e293b";
    ctx.fillRect(16, y, W - 32, 14);
    ctx.fillStyle = theme.accent;
    ctx.fillRect(16, y, (W - 32) * Math.max(0, Math.min(100, model.progress)) / 100, 14);
    ctx.fillStyle = "#0a0e17";
    ctx.font = "bold 11px monospace";
    ctx.fillText(`${Math.round(model.progress)}%`, 22, y + 11);
    y += 26;
  } else {
    y += 6;
  }

  // Divider
  ctx.strokeStyle = "#1e293b";
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(16, y);
  ctx.lineTo(W - 16, y);
  ctx.stroke();
  y += 22;

  // Findings (genuine persisted analysis / live metrics)
  if (model.findings.length === 0) {
    ctx.font = "14px sans-serif";
    ctx.fillStyle = "#475569";
    ctx.fillText("No results yet.", 16, y);
  } else {
    for (const f of model.findings) {
      if (y > H - 14) break;
      ctx.font = "bold 12px sans-serif";
      ctx.fillStyle = theme.accent;
      ctx.fillText(f.label.toUpperCase(), 16, y);
      y += 18;
      ctx.font = "14px sans-serif";
      ctx.fillStyle = "#cbd5e1";
      const lines = wrapLines(ctx, f.value, W - 32, 2);
      for (const ln of lines) {
        if (y > H - 10) break;
        ctx.fillText(ln, 16, y);
        y += 19;
      }
      y += 6;
    }
  }
}

function drawBoardroomScreen(ctx: CanvasRenderingContext2D, speaker: string | null, round: number | null) {
  const W = 512;
  ctx.fillStyle = "#0a0e17";
  ctx.fillRect(0, 0, W, 320);
  ctx.fillStyle = "#3b0764";
  ctx.fillRect(0, 0, W, 44);
  ctx.fillStyle = "#ffffff";
  ctx.font = "bold 18px sans-serif";
  ctx.fillText("⚖️ BOARDROOM DEBATE", 16, 30);
  ctx.fillStyle = "#a78bfa";
  ctx.font = "bold 16px sans-serif";
  ctx.fillText(`STATUS: ${round ? `ROUND ${round}` : "STRATEGIC DEBATE"}`, 20, 92);
  ctx.fillStyle = "#f3e8ff";
  ctx.font = "15px sans-serif";
  ctx.fillText(`SPEAKER: ${speaker || "—"}`, 20, 134);
}

function Monitor3DComponent({
  type,
  position = [0, 0, 0],
  rotation = [0, 0, 0],
  scale = 1,
  activeSpeaker,
  currentRound,
}: Monitor3DProps) {
  const textureRef = useRef<THREE.CanvasTexture | null>(null);

  const agents = useSim((s) => s.agents);
  const executionState = useSim((s) => s.executionState);
  const plans = useSim((s) => s.plans);
  const startup = useSim((s) => s.startup);
  const debates = useSim((s) => s.debates);

  const canvas = useMemo(() => {
    const c = document.createElement("canvas");
    c.width = 512;
    c.height = 320;
    return c;
  }, []);

  const texture = useMemo(() => {
    const tex = new THREE.CanvasTexture(canvas);
    tex.minFilter = THREE.LinearFilter;
    tex.magFilter = THREE.LinearFilter;
    textureRef.current = tex;
    return tex;
  }, [canvas]);

  const model = useMemo<ScreenModel | null>(() => {
    if (type === "BOARDROOM") return null;
    return buildModel(type, { agents, executionState, plans, startup, debates });
  }, [type, agents, executionState, plans, startup, debates]);

  // Redraw the screen texture only when the underlying data changes — no
  // per-frame animation, so the monitor never flickers or churns the GPU.
  useEffect(() => {
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    if (type === "BOARDROOM") {
      drawBoardroomScreen(ctx, activeSpeaker ?? null, currentRound ?? null);
    } else if (model) {
      drawDepartmentScreen(ctx, type, model);
    }
    if (textureRef.current) textureRef.current.needsUpdate = true;
  }, [canvas, type, model, activeSpeaker, currentRound]);

  const glow =
    type === "DEVELOPMENT" ? "#06b6d4"
    : type === "MARKETING" ? "#ec4899"
    : type === "FINANCE" ? "#10b981"
    : type === "BOARDROOM" ? "#a78bfa"
    : "#f59e0b";

  return (
    <group position={position} rotation={rotation} scale={scale}>
      {/* Sleek Metallic Monitor Stand */}
      <mesh position={[0, -0.22, 0]} castShadow>
        <boxGeometry args={[0.26, 0.02, 0.2]} />
        <meshStandardMaterial color="#1e293b" roughness={0.2} metalness={0.8} />
      </mesh>
      <mesh position={[0, -0.1, -0.05]} rotation={[0.08, 0, 0]} castShadow>
        <boxGeometry args={[0.045, 0.26, 0.045]} />
        <meshStandardMaterial color="#475569" roughness={0.2} metalness={0.9} />
      </mesh>

      {/* Thin Bezel Display Casing */}
      <mesh position={[0, 0.14, 0]} castShadow>
        <boxGeometry args={[0.76, 0.46, 0.035]} />
        <meshStandardMaterial color="#0f172a" roughness={0.2} metalness={0.8} />
      </mesh>

      {/* Emissive Display Screen Mesh */}
      <mesh position={[0, 0.14, 0.02]}>
        <planeGeometry args={[0.72, 0.42]} />
        <meshBasicMaterial map={texture} toneMapped={false} />
      </mesh>

      {/* Soft Emissive Screen Light Glow */}
      <pointLight position={[0, 0.14, 0.25]} intensity={0.8} distance={1.8} color={glow} />
    </group>
  );
}

export default memo(Monitor3DComponent);
