import { memo } from "react";
import { ROOM_LAYOUTS } from "./officeLayout";
import {
  ExecutiveDesk3D,
  Chair3D,
  Whiteboard3D,
  Plant3D,
  Bookshelf3D,
  CeilingLight3D,
  RoomSign3D,
} from "./OfficeFurniture3D";
import Monitor3D from "./Monitor3D";

function CEOOffice3DComponent({
  onSelectDepartment,
  active,
}: {
  onSelectDepartment?: (dept: string) => void;
  active?: boolean;
}) {
  const room = ROOM_LAYOUTS.CEO;

  return (
    <group
      position={[room.center.x, room.center.y, room.center.z]}
      onClick={(e) => {
        e.stopPropagation();
        if (onSelectDepartment) onSelectDepartment("CEO");
      }}
    >
      {/* Ceiling Light Fixture */}
      <CeilingLight3D type="CEO" color="#fbbf24" position={[0, 2.2, 0]} />

      {/* CEO Executive Desk — Keyboard on CEO side (South), modesty panel on room side (North) */}
      <ExecutiveDesk3D position={[0, 0, -0.55]} rotation={[0, 0, 0]} color="#5c3a1e" />

      {/* CEO Executive Swivel Chair — Facing South (+Z) centered under CEO pelvis */}
      <Chair3D position={[0, 0, -1.22]} rotation={[0, 0, 0]} color="#5c3a1e" scale={1.05} />

      {/* Single Primary Animated Monitor on desk facing CEO (screen normal facing North -Z) */}
      <Monitor3D type="CEO" position={[0, 0.78, -0.35]} rotation={[0, Math.PI, 0]} scale={0.85} />

      {/* Strategy Whiteboard / World Map */}
      <Whiteboard3D position={[-2.4, 0, 0.4]} rotation={[0, Math.PI / 2, 0]} title="STRATEGY & ROADMAP" />

      {/* Executive Bookshelf against back wall out of camera line-of-sight */}
      <Bookshelf3D position={[2.0, 0, -1.55]} rotation={[0, 0, 0]} />

      {/* Executive Potted Plants framing room */}
      <Plant3D position={[2.3, 0, 1.2]} scale={0.95} />
      <Plant3D position={[-2.3, 0, 1.2]} scale={0.75} />

      {/* Framed artwork on back wall */}
      <mesh position={[0, 1.5, -1.85]} castShadow>
        <boxGeometry args={[1.0, 0.6, 0.03]} />
        <meshStandardMaterial color="#374151" roughness={0.3} metalness={0.5} />
      </mesh>
      <mesh position={[0, 1.5, -1.83]}>
        <planeGeometry args={[0.9, 0.5]} />
        <meshStandardMaterial color="#1e3a5f" roughness={0.4} />
      </mesh>

      {/* Physical 3D Wall Signage */}
      <RoomSign3D position={[0, 1.85, 1.85]} title="CEO SUITE" color="#f59e0b" />

      {/* CEO Room Warm Ambient (No castShadow overhead) */}
      <pointLight position={[0, 2.0, 0]} color="#f59e0b" intensity={active ? 2.2 : 1.2} distance={5.5} />
      <pointLight position={[-1.5, 1.5, -1.0]} color="#fde68a" intensity={0.6} distance={3} />
    </group>
  );
}

export default memo(CEOOffice3DComponent);
