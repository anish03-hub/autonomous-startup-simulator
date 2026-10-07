import { memo } from "react";
import { ROOM_LAYOUTS } from "./officeLayout";
import Workstation3D from "./Workstation3D";
import ServerRack3D from "./ServerRack3D";
import { Whiteboard3D, Plant3D, CeilingLight3D, RoomSign3D } from "./OfficeFurniture3D";

function DevelopmentOffice3DComponent({
  onSelectDepartment,
  active,
}: {
  onSelectDepartment?: (dept: string) => void;
  active?: boolean;
}) {
  const room = ROOM_LAYOUTS.DEVELOPMENT;

  return (
    <group
      position={[room.center.x, room.center.y, room.center.z]}
      onClick={(e) => {
        e.stopPropagation();
        if (onSelectDepartment) onSelectDepartment("DEVELOPMENT");
      }}
    >
      {/* Ceiling LED Fixture */}
      <CeilingLight3D type="DEV" color="#22d3ee" position={[0, 2.2, 0]} />

      {/* Workstation 1 (Dev Lead) — Local Anchor */}
      <Workstation3D
        position={[0.55, 0, -1.05]}
        rotation={[0, -Math.PI / 2, 0]}
        department="DEVELOPMENT"
        deskColor="#3d2b1a"
        chairColor="#0284c7"
      />

      {/* Workstation 2 (Backend Eng) — Local Anchor */}
      <Workstation3D
        position={[0.55, 0, 1.05]}
        rotation={[0, -Math.PI / 2, 0]}
        department="DEVELOPMENT"
        deskColor="#3d2b1a"
        chairColor="#0369a1"
      />

      {/* Technical / Server Zone (East Wall Anchor) */}
      <ServerRack3D position={[-1.7, 0, -1.1]} rotation={[0, Math.PI / 2, 0]} />
      <Whiteboard3D position={[-1.7, 0, 1.1]} rotation={[0, Math.PI / 2, 0]} title="SYSTEM ARCHITECTURE" />

      {/* Office Plants (Corner Placement) */}
      <Plant3D position={[1.8, 0, 1.8]} scale={0.75} />
      <Plant3D position={[-1.8, 0, -1.8]} scale={0.55} />

      {/* 3D Physical Wall Signage */}
      <RoomSign3D position={[-2.1, 1.85, 0]} rotation={[0, Math.PI / 2, 0]} title="DEVELOPMENT" color="#06b6d4" />

      {/* Cable Tray under Desk Legs */}
      <mesh position={[0.55, 0.02, 0]} castShadow>
        <boxGeometry args={[0.15, 0.04, 2.8]} />
        <meshStandardMaterial color="#374151" roughness={0.4} metalness={0.6} />
      </mesh>

      {/* Cyan Lighting Accent */}
      <pointLight position={[0, 2.0, 0]} color="#06b6d4" intensity={active ? 2.2 : 1.0} distance={5.5} />
      <pointLight position={[1.0, 1.2, 0]} color="#22d3ee" intensity={0.4} distance={3} />
    </group>
  );
}

export default memo(DevelopmentOffice3DComponent);
