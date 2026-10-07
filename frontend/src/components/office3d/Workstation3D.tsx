import { memo } from "react";
import { Desk3D, Chair3D } from "./OfficeFurniture3D";
import Monitor3D from "./Monitor3D";
import type { AgentType } from "../../types";

interface Workstation3DProps {
  position?: [number, number, number];
  rotation?: [number, number, number];
  department: AgentType;
  deskColor?: string;
  chairColor?: string;
}

function Workstation3DComponent({
  position = [0, 0, 0],
  rotation = [0, 0, 0],
  department,
  deskColor = "#3d2b1a",
  chairColor = "#2d3748",
}: Workstation3DProps) {
  return (
    <group position={position} rotation={rotation}>
      {/* Desk */}
      <Desk3D position={[0, 0, 0]} color={deskColor} />
      {/* Office Chair — Positioned directly behind seated employee pelvis, facing desk & monitor */}
      <Chair3D position={[0, 0, 0.52]} rotation={[0, Math.PI, 0]} color={chairColor} />
      {/* Monitor — lowered, pushed back and shrunk so the (now larger) seated
          employee's head/shoulders/arms read clearly above the screen bezel. */}
      <Monitor3D
        type={department}
        position={[0, 0.72, -0.26]}
        rotation={[0, 0, 0]}
        scale={0.72}
      />
    </group>
  );
}

export default memo(Workstation3DComponent);
