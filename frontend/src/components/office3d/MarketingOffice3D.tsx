import { memo } from "react";
import { ROOM_LAYOUTS } from "./officeLayout";
import Workstation3D from "./Workstation3D";
import { Whiteboard3D, Plant3D, CeilingLight3D, RoomSign3D } from "./OfficeFurniture3D";

function MarketingOffice3DComponent({
  onSelectDepartment,
  active,
}: {
  onSelectDepartment?: (dept: string) => void;
  active?: boolean;
}) {
  const room = ROOM_LAYOUTS.MARKETING;

  return (
    <group
      position={[room.center.x, room.center.y, room.center.z]}
      onClick={(e) => {
        e.stopPropagation();
        if (onSelectDepartment) onSelectDepartment("MARKETING");
      }}
    >
      {/* Ceiling Pendant Lights */}
      <CeilingLight3D type="MKT" color="#f472b6" position={[0, 2.2, 0]} />

      {/* Mkt 1 Workstation */}
      <Workstation3D
        position={[-0.7, 0, -1.3]}
        rotation={[0, Math.PI / 2, 0]}
        department="MARKETING"
        deskColor="#3d2b1a"
        chairColor="#be185d"
      />

      {/* Mkt 2 Workstation */}
      <Workstation3D
        position={[-0.7, 0, 1.3]}
        rotation={[0, Math.PI / 2, 0]}
        department="MARKETING"
        deskColor="#3d2b1a"
        chairColor="#9d174d"
      />

      {/* Campaign Board */}
      <Whiteboard3D position={[1.8, 0, -1.2]} rotation={[0, -Math.PI / 2, 0]} title="CAMPAIGN & FUNNEL" />

      {/* Brand Board */}
      <Whiteboard3D position={[1.8, 0, 1.2]} rotation={[0, -Math.PI / 2, 0]} title="BRAND POSITIONING" />

      {/* Potted Plants */}
      <Plant3D position={[-1.8, 0, 1.8]} scale={0.75} />
      <Plant3D position={[1.8, 0, -1.8]} scale={0.55} />

      {/* Lounge accent — small side table */}
      <group position={[-1.6, 0, 0]}>
        <mesh position={[0, 0.35, 0]} castShadow>
          <cylinderGeometry args={[0.25, 0.25, 0.04, 12]} />
          <meshStandardMaterial color="#b8956e" roughness={0.4} />
        </mesh>
        <mesh position={[0, 0.17, 0]} castShadow>
          <cylinderGeometry args={[0.03, 0.03, 0.34, 8]} />
          <meshStandardMaterial color="#475569" roughness={0.3} metalness={0.7} />
        </mesh>
        <mesh position={[0, 0.38, 0.05]} castShadow>
          <cylinderGeometry args={[0.035, 0.03, 0.07, 10]} />
          <meshStandardMaterial color="#f8fafc" roughness={0.2} />
        </mesh>
      </group>

      {/* 3D Physical Wall Signage */}
      <RoomSign3D position={[2.1, 1.85, 0]} rotation={[0, -Math.PI / 2, 0]} title="MARKETING" color="#ec4899" />

      {/* Soft Magenta Lighting Accent */}
      <pointLight position={[0, 2.0, 0]} color="#ec4899" intensity={active ? 2.2 : 1.0} distance={5.5} />
      <pointLight position={[-0.8, 1.2, 0]} color="#f9a8d4" intensity={0.4} distance={3} />
    </group>
  );
}

export default memo(MarketingOffice3DComponent);
