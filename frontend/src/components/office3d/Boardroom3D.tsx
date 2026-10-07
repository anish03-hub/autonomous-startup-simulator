import { memo } from "react";
import { ROOM_LAYOUTS } from "./officeLayout";
import { ConferenceTable3D, Chair3D, Plant3D, CeilingLight3D, RoomSign3D } from "./OfficeFurniture3D";
import Monitor3D from "./Monitor3D";

interface Boardroom3DProps {
  onOpenBoardroom?: () => void;
  activeSpeaker?: string | null;
  currentRound?: number | null;
  hasDebate?: boolean;
}

function Boardroom3DComponent({
  onOpenBoardroom,
  activeSpeaker,
  currentRound,
  hasDebate,
}: Boardroom3DProps) {
  const room = ROOM_LAYOUTS.BOARDROOM;

  return (
    <group
      position={[room.center.x, room.center.y, room.center.z]}
      onClick={(e) => {
        e.stopPropagation();
        if (onOpenBoardroom) onOpenBoardroom();
      }}
    >
      {/* Ceiling Ring Light Chandelier Fixture */}
      <CeilingLight3D type="BOARDROOM" color="#a78bfa" position={[0, 2.3, 0]} />

      {/* Large Oval Conference Table */}
      <ConferenceTable3D position={[0, 0, 0]} />

      {/* 5 Boardroom Executive Chairs around Table */}
      <Chair3D position={[0, 0, -1.2]} rotation={[0, 0, 0]} color="#4c1d95" scale={1.1} />
      <Chair3D position={[1.35, 0, -0.6]} rotation={[0, -Math.PI / 3, 0]} color="#3b1f7a" scale={1.05} />
      <Chair3D position={[1.35, 0, 0.6]} rotation={[0, (-2 * Math.PI) / 3, 0]} color="#3b1f7a" scale={1.05} />
      <Chair3D position={[-1.35, 0, -0.6]} rotation={[0, Math.PI / 3, 0]} color="#3b1f7a" scale={1.05} />
      <Chair3D position={[-1.35, 0, 0.6]} rotation={[0, (2 * Math.PI) / 3, 0]} color="#3b1f7a" scale={1.05} />

      {/* Presentation Screen mounted on Wall */}
      <group position={[0, 1.6, -2.0]} rotation={[0, 0, 0]}>
        {/* Screen bezel */}
        <mesh position={[0, 0, 0]} castShadow>
          <boxGeometry args={[2.8, 1.6, 0.06]} />
          <meshStandardMaterial color="#1e1b2e" roughness={0.2} metalness={0.85} />
        </mesh>
        <Monitor3D
          type="BOARDROOM"
          position={[0, 0, 0.04]}
          scale={3.2}
          activeSpeaker={activeSpeaker}
          currentRound={currentRound}
        />
      </group>

      {/* Plants in corners */}
      <Plant3D position={[-2.7, 0, -1.8]} scale={0.85} />
      <Plant3D position={[2.7, 0, -1.8]} scale={0.7} />

      {/* 3D Physical Wall Signage */}
      <RoomSign3D position={[0, 1.85, 2.05]} title="BOARDROOM" color="#8b5cf6" />

      {/* Central Boardroom Spotlight & Pulsing Debate Glow (No castShadow overhead) */}
      <pointLight
        position={[0, 2.6, 0]}
        color={hasDebate ? "#a78bfa" : "#8b5cf6"}
        intensity={hasDebate ? 3.0 : 1.4}
        distance={7}
      />
      {/* Warm fill to brighten the table area */}
      <pointLight position={[0, 1.5, 0]} color="#ede9fe" intensity={0.5} distance={4} />
    </group>
  );
}

export default memo(Boardroom3DComponent);
