import { useRef, useMemo, memo } from "react";
import { useFrame } from "@react-three/fiber";
import * as THREE from "three";
import type { AgentType } from "../../types";

interface Monitor3DProps {
  type: AgentType | "BOARDROOM";
  position?: [number, number, number];
  rotation?: [number, number, number];
  scale?: number;
  activeSpeaker?: string | null;
  currentRound?: number | null;
}

const TYPE_FRAME_OFFSETS: Record<string, number> = {
  DEVELOPMENT: 0,
  MARKETING: 2,
  FINANCE: 4,
  CEO: 1,
  BOARDROOM: 3,
};

function Monitor3DComponent({
  type,
  position = [0, 0, 0],
  rotation = [0, 0, 0],
  scale = 1,
  activeSpeaker,
  currentRound,
}: Monitor3DProps) {
  const textureRef = useRef<THREE.CanvasTexture | null>(null);
  const frameCount = useRef(TYPE_FRAME_OFFSETS[type] || 0);

  // Dynamic canvas texture for monitor display (512x320)
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

  // Throttled monitor screen update loop (~10 FPS instead of 60 FPS per monitor)
  useFrame(({ clock }) => {
    frameCount.current += 1;
    if (frameCount.current % 6 !== 0) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    const time = clock.getElapsedTime();

    // Dark IDE / Dashboard background
    ctx.fillStyle = "#090d16";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    if (type === "DEVELOPMENT") {
      // IDE Header Bar
      ctx.fillStyle = "#0f172a";
      ctx.fillRect(0, 0, canvas.width, 36);
      ctx.fillStyle = "#38bdf8";
      ctx.font = "bold 15px monospace";
      ctx.fillText("⚡ VS_CODE // main.ts", 18, 24);

      // Line Numbers & Real Code Editor Output
      ctx.font = "14px monospace";

      const lines = [
        "1  function analyzeStartup() {",
        "2    const mvp = evaluateScope();",
        "3    const architecture = buildStack();",
        "4    const cluster = deployNodes();",
        "5    return buildPlan(mvp);",
        "6  }",
        "7  ",
        "8  // API Gateway connected [200 OK]",
        "9  // DB Latency: 1.4ms (Healthy)",
        "10 export default analyzeStartup();",
      ];

      const scrollY = (time * 18) % 120;
      ctx.save();
      ctx.beginPath();
      ctx.rect(0, 36, canvas.width, canvas.height - 36);
      ctx.clip();

      lines.forEach((line, idx) => {
        const y = 62 + idx * 24 - (scrollY % 120);
        const finalY = y < 36 ? y + 120 : y;

        if (line.includes("function") || line.includes("return") || line.includes("export")) {
          ctx.fillStyle = "#38bdf8";
        } else if (line.includes("const")) {
          ctx.fillStyle = "#f472b6";
        } else if (line.includes("//")) {
          ctx.fillStyle = "#10b981";
        } else {
          ctx.fillStyle = "#cbd5e1";
        }
        ctx.fillText(line, 18, finalY);
      });
      ctx.restore();

      // Blinking IDE Cursor
      if (Math.sin(time * 8) > 0) {
        ctx.fillStyle = "#38bdf8";
        ctx.fillRect(270, 158, 8, 16);
      }
    } else if (type === "MARKETING") {
      // Campaign Analytics Screen
      ctx.fillStyle = "#1e102a";
      ctx.fillRect(0, 0, canvas.width, 36);
      ctx.fillStyle = "#f472b6";
      ctx.font = "bold 16px sans-serif";
      ctx.fillText("📈 CAMPAIGN ANALYTICS", 18, 24);

      // Metrics Cards
      ctx.fillStyle = "#33153b";
      ctx.fillRect(20, 50, 140, 60);
      ctx.fillRect(180, 50, 140, 60);
      ctx.fillRect(340, 50, 140, 60);

      ctx.fillStyle = "#ffffff";
      ctx.font = "12px sans-serif";
      ctx.fillText("Conversion", 30, 68);
      ctx.fillText("CTR", 190, 68);
      ctx.fillText("Growth", 350, 68);

      ctx.font = "bold 18px sans-serif";
      ctx.fillStyle = "#ec4899";
      ctx.fillText("68%", 30, 95);
      ctx.fillStyle = "#38bdf8";
      ctx.fillText("4.7%", 190, 95);
      ctx.fillStyle = "#10b981";
      ctx.fillText("+18%", 350, 95);

      // Animated Line Chart
      ctx.strokeStyle = "#ec4899";
      ctx.lineWidth = 3;
      ctx.beginPath();
      for (let x = 20; x < canvas.width - 20; x += 10) {
        const y = 240 - Math.sin((x / 25) + time * 3) * 35 - (x / canvas.width) * 40;
        if (x === 20) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();
    } else if (type === "FINANCE") {
      // Financial Control Ledger
      ctx.fillStyle = "#062e24";
      ctx.fillRect(0, 0, canvas.width, 36);
      ctx.fillStyle = "#34d399";
      ctx.font = "bold 16px sans-serif";
      ctx.fillText("💵 FINANCIAL RUNWAY & BUDGET", 18, 24);

      ctx.font = "15px monospace";
      const rows = [
        ["REVENUE", "$45,200 / mo"],
        ["BURN RATE", "$28,500 / mo"],
        ["RUNWAY", "18.4 Months"],
        ["BUDGET", "$500,000 Total"],
        ["NET MARGIN", "+24.2% Growth"],
      ];

      rows.forEach(([label, val], idx) => {
        ctx.fillStyle = "#a7f3d0";
        ctx.fillText(label, 24, 75 + idx * 42);
        ctx.fillStyle = label === "REVENUE" || label === "NET MARGIN" ? "#34d399" : "#fbbf24";
        ctx.fillText(val, 280, 75 + idx * 42);
      });
    } else if (type === "CEO") {
      // Executive Strategic Dashboard
      ctx.fillStyle = "#451a03";
      ctx.fillRect(0, 0, canvas.width, 36);
      ctx.fillStyle = "#fbbf24";
      ctx.font = "bold 16px sans-serif";
      ctx.fillText("🎯 STRATEGY & OBJECTIVES", 18, 24);

      ctx.font = "bold 15px sans-serif";
      ctx.fillStyle = "#fef3c7";
      ctx.fillText("STRATEGY: Product-Market Fit", 24, 75);
      ctx.fillText("MVP: Autonomous AI Suite", 24, 115);
      ctx.fillText("MARKET: B2B Enterprise SaaS", 24, 155);
      ctx.fillText("OBJECTIVES: 100 Early Adopters", 24, 195);

      // Strategic Readiness Bar
      ctx.fillStyle = "#292524";
      ctx.fillRect(24, 235, 460, 24);
      const w = 460 * (0.72 + Math.sin(time * 0.5) * 0.03);
      ctx.fillStyle = "#f59e0b";
      ctx.fillRect(24, 235, w, 24);
      ctx.fillStyle = "#ffffff";
      ctx.font = "bold 12px sans-serif";
      ctx.fillText("GO-TO-MARKET READINESS: 72%", 170, 252);
    } else if (type === "BOARDROOM") {
      // Big Wall Display
      ctx.fillStyle = "#3b0764";
      ctx.fillRect(0, 0, canvas.width, 44);
      ctx.fillStyle = "#ffffff";
      ctx.font = "bold 18px sans-serif";
      ctx.fillText("AUTONOMOUS BOARDROOM DEBATE", 18, 30);

      ctx.fillStyle = "#a78bfa";
      ctx.font = "bold 16px sans-serif";
      ctx.fillText(`STATUS: ${currentRound ? `ROUND ${currentRound}` : "STRATEGIC DEBATE"}`, 24, 95);

      ctx.fillStyle = "#f3e8ff";
      ctx.font = "15px sans-serif";
      ctx.fillText(`SPEAKER: ${activeSpeaker || "CEO Alex Vance"}`, 24, 140);
    }

    if (textureRef.current) {
      textureRef.current.needsUpdate = true;
    }
  });

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
        <meshBasicMaterial map={texture} />
      </mesh>

      {/* Soft Emissive Screen Light Glow (No shadow overhead) */}
      <pointLight
        position={[0, 0.14, 0.25]}
        intensity={
          type === "DEVELOPMENT"
            ? 0.9
            : type === "MARKETING"
            ? 0.8
            : type === "FINANCE"
            ? 0.7
            : type === "BOARDROOM"
            ? 1.2
            : 0.85
        }
        distance={1.8}
        color={
          type === "DEVELOPMENT"
            ? "#06b6d4"
            : type === "MARKETING"
            ? "#ec4899"
            : type === "FINANCE"
            ? "#10b981"
            : type === "BOARDROOM"
            ? "#a78bfa"
            : "#f59e0b"
        }
      />
    </group>
  );
}

export default memo(Monitor3DComponent);
