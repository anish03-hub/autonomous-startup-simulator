import { useRef, memo } from "react";
import { useFrame } from "@react-three/fiber";
import { Text } from "@react-three/drei";
import * as THREE from "three";

function ServerRack3DComponent({
  position = [0, 0, 0],
  rotation = [0, 0, 0],
}: {
  position?: [number, number, number];
  rotation?: [number, number, number];
}) {
  const ledGroupRef = useRef<THREE.Group>(null);
  const frameCount = useRef(0);

  useFrame(({ clock }) => {
    frameCount.current += 1;
    if (frameCount.current % 4 !== 0) return; // Throttle LED blink updates to ~15 FPS

    const t = clock.getElapsedTime();
    if (ledGroupRef.current) {
      ledGroupRef.current.children.forEach((child, i) => {
        if (child instanceof THREE.Mesh && child.material) {
          const blink = Math.sin(t * (4 + (i % 5))) > 0;
          (child.material as THREE.MeshBasicMaterial).opacity = blink ? 1 : 0.2;
        }
      });
    }
  });

  return (
    <group position={position} rotation={rotation}>
      {/* Outer Industrial Metal Frame */}
      <mesh position={[0, 1.2, 0]} castShadow receiveShadow>
        <boxGeometry args={[0.95, 2.4, 0.85]} />
        <meshStandardMaterial color="#0b1120" roughness={0.3} metalness={0.9} />
      </mesh>

      {/* Front Glass Door Frame (Standard transparent material — NO expensive transmission pass) */}
      <mesh position={[0, 1.2, 0.43]}>
        <planeGeometry args={[0.86, 2.3]} />
        <meshStandardMaterial
          color="#0284c7"
          transparent
          opacity={0.35}
          roughness={0.1}
          metalness={0.5}
        />
      </mesh>

      {/* Rack Server Blade Units (U-Units) */}
      {[0.3, 0.6, 0.9, 1.2, 1.5, 1.8, 2.1].map((y, idx) => (
        <group key={idx} position={[0, y, 0.05]}>
          {/* Blade Unit */}
          <mesh castShadow>
            <boxGeometry args={[0.85, 0.22, 0.72]} />
            <meshStandardMaterial color="#1e293b" roughness={0.4} metalness={0.8} />
          </mesh>

          {/* Grille Vent */}
          <mesh position={[0, 0, 0.365]}>
            <planeGeometry args={[0.55, 0.16]} />
            <meshStandardMaterial color="#0f172a" roughness={0.8} />
          </mesh>
        </group>
      ))}

      {/* Server Rack LED Status Display Panel */}
      <group position={[0, 2.1, 0.435]}>
        <Text
          position={[0, 0.05, 0]}
          fontSize={0.045}
          color="#22d3ee"
          anchorX="center"
          anchorY="middle"
        >
          API ONLINE
        </Text>
        <Text
          position={[0, 0, 0]}
          fontSize={0.045}
          color="#10b981"
          anchorX="center"
          anchorY="middle"
        >
          DATABASE ONLINE
        </Text>
        <Text
          position={[0, -0.05, 0]}
          fontSize={0.045}
          color="#38bdf8"
          anchorX="center"
          anchorY="middle"
        >
          WORKERS ONLINE
        </Text>
      </group>

      {/* Blinking LEDs Panel */}
      <group ref={ledGroupRef} position={[0, 0, 0.435]}>
        {[0.3, 0.6, 0.9, 1.2, 1.5, 1.8].map((y, rowIdx) => (
          <group key={rowIdx} position={[0, y, 0]}>
            {[-0.34, -0.3, -0.26, 0.3, 0.34].map((x, colIdx) => {
              const color = colIdx === 4 ? "#ef4444" : colIdx % 2 === 0 ? "#06b6d4" : "#10b981";
              return (
                <mesh key={colIdx} position={[x, 0, 0]}>
                  <sphereGeometry args={[0.018, 8, 8]} />
                  <meshBasicMaterial color={color} transparent opacity={1} />
                </mesh>
              );
            })}
          </group>
        ))}
      </group>

      {/* Server Rack Glow Light (No shadow overhead) */}
      <pointLight position={[0, 1.2, 0.65]} color="#06b6d4" intensity={1.5} distance={2.8} />
    </group>
  );
}

export default memo(ServerRack3DComponent);
