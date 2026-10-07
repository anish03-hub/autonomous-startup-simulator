import { memo } from "react";
import { ROOM_LAYOUTS } from "./officeLayout";
import { Desk3D, Chair3D, Whiteboard3D, Plant3D, CeilingLight3D, RoomSign3D } from "./OfficeFurniture3D";
import Monitor3D from "./Monitor3D";

function FinanceOffice3DComponent({
  onSelectDepartment,
  active,
}: {
  onSelectDepartment?: (dept: string) => void;
  active?: boolean;
}) {
  const room = ROOM_LAYOUTS.FINANCE;

  return (
    <group
      position={[room.center.x, room.center.y, room.center.z]}
      onClick={(e) => {
        e.stopPropagation();
        if (onSelectDepartment) onSelectDepartment("FINANCE");
      }}
    >
      {/* Recessed Ceiling LED Fixture */}
      <CeilingLight3D type="FIN" color="#34d399" position={[0, 2.2, 0]} />

      {/* ─── FIN 1 WORKSTATION (Left / West) ─── */}
      {/* Desk */}
      <Desk3D position={[-1.8, 0, 0.55]} rotation={[0, 0, 0]} color="#3d2b1a" />
      {/* Swivel Chair centered under emp-fin-1 pelvis */}
      <Chair3D position={[-1.8, 0, 1.22]} rotation={[0, Math.PI, 0]} color="#047857" scale={1.05} />
      {/* Primary Animated Monitor facing South (+Z) toward emp-fin-1 */}
      <Monitor3D type="FINANCE" position={[-1.8, 0.72, 0.35]} rotation={[0, 0, 0]} scale={0.80} />

      {/* ─── FIN 2 WORKSTATION (Right / East) ─── */}
      {/* Desk */}
      <Desk3D position={[1.8, 0, 0.55]} rotation={[0, 0, 0]} color="#3d2b1a" />
      {/* Swivel Chair centered under emp-fin-2 pelvis */}
      <Chair3D position={[1.8, 0, 1.22]} rotation={[0, Math.PI, 0]} color="#065f46" scale={1.05} />
      {/* Primary Animated Monitor facing South (+Z) toward emp-fin-2 */}
      <Monitor3D type="FINANCE" position={[1.8, 0.72, 0.35]} rotation={[0, 0, 0]} scale={0.80} />

      {/* Financial Ledger Board */}
      <Whiteboard3D position={[0, 0, -1.8]} rotation={[0, 0, 0]} title="BUDGET & RUNWAY FORECAST" />

      {/* Filing Cabinet */}
      <group position={[-2.5, 0, -1.4]}>
        <mesh position={[0, 0.6, 0]} castShadow>
          <boxGeometry args={[0.6, 1.2, 0.5]} />
          <meshStandardMaterial color="#6b7280" roughness={0.35} metalness={0.7} />
        </mesh>
        {/* Drawer handles */}
        {[0.3, 0.6, 0.9].map((y) => (
          <mesh key={y} position={[0, y, 0.26]} castShadow>
            <boxGeometry args={[0.2, 0.02, 0.02]} />
            <meshStandardMaterial color="#d1d5db" roughness={0.2} metalness={0.8} />
          </mesh>
        ))}
      </group>

      {/* Safe / secure storage */}
      <group position={[2.5, 0, -1.4]}>
        <mesh position={[0, 0.45, 0]} castShadow>
          <boxGeometry args={[0.55, 0.9, 0.5]} />
          <meshStandardMaterial color="#374151" roughness={0.25} metalness={0.85} />
        </mesh>
        {/* Lock dial */}
        <mesh position={[0.15, 0.5, 0.26]} castShadow>
          <cylinderGeometry args={[0.05, 0.05, 0.02, 12]} />
          <meshStandardMaterial color="#f59e0b" roughness={0.2} metalness={0.9} />
        </mesh>
      </group>

      {/* Potted Plant */}
      <Plant3D position={[-2.5, 0, 1.4]} scale={0.75} />

      {/* Printer */}
      <group position={[2.5, 0, 1.0]}>
        <mesh position={[0, 0.4, 0]} castShadow>
          <boxGeometry args={[0.5, 0.3, 0.4]} />
          <meshStandardMaterial color="#e2e8f0" roughness={0.3} />
        </mesh>
        <mesh position={[0, 0.56, -0.05]}>
          <boxGeometry args={[0.44, 0.02, 0.2]} />
          <meshStandardMaterial color="#cbd5e1" roughness={0.3} />
        </mesh>
        {/* Paper tray */}
        <mesh position={[0, 0.32, 0.22]}>
          <boxGeometry args={[0.35, 0.04, 0.08]} />
          <meshStandardMaterial color="#f8fafc" roughness={0.6} />
        </mesh>
        {/* Stand */}
        <mesh position={[0, 0.13, 0]} castShadow>
          <boxGeometry args={[0.45, 0.25, 0.35]} />
          <meshStandardMaterial color="#94a3b8" roughness={0.4} metalness={0.5} />
        </mesh>
      </group>

      {/* 3D Physical Wall Signage */}
      <RoomSign3D position={[0, 1.85, -1.85]} title="FINANCE & OPS" color="#10b981" />

      {/* Emerald Green Lighting Accent */}
      <pointLight position={[0, 2.0, 0]} color="#10b981" intensity={active ? 2.2 : 1.0} distance={5.5} />
      <pointLight position={[0, 1.2, 0.8]} color="#6ee7b7" intensity={0.35} distance={3} />
    </group>
  );
}

export default memo(FinanceOffice3DComponent);
