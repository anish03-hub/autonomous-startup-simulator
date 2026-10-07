import type { AgentType } from "../../types";

export interface Position3D {
  x: number;
  y: number;
  z: number;
}

export interface RoomLayout {
  id: AgentType | "BOARDROOM";
  name: string;
  center: Position3D;
  size: [number, number]; // width (X), length (Z)
  color: string; // accent color
  lightColor: string;
  doorway: Position3D;
  floorColor: string;
}

export const ROOM_LAYOUTS: Record<AgentType | "BOARDROOM", RoomLayout> = {
  CEO: {
    id: "CEO",
    name: "CEO Suite",
    center: { x: 0, y: 0, z: -4.2 },
    size: [5.5, 3.8],
    color: "#f59e0b",
    lightColor: "#fbbf24",
    doorway: { x: 0, y: 0, z: -2.3 },
    floorColor: "#291a10",
  },
  BOARDROOM: {
    id: "BOARDROOM",
    name: "Boardroom",
    center: { x: 0, y: 0, z: 0 },
    size: [6.2, 4.2],
    color: "#8b5cf6",
    lightColor: "#a78bfa",
    doorway: { x: 0, y: 0, z: 2.1 },
    floorColor: "#1a102f",
  },
  DEVELOPMENT: {
    id: "DEVELOPMENT",
    name: "Development Bay",
    center: { x: 5.2, y: 0, z: 0 },
    size: [4.8, 4.2],
    color: "#06b6d4",
    lightColor: "#22d3ee",
    doorway: { x: 3.1, y: 0, z: 0 },
    floorColor: "#0f172a",
  },
  MARKETING: {
    id: "MARKETING",
    name: "Marketing Hub",
    center: { x: -5.2, y: 0, z: 0 },
    size: [4.8, 4.2],
    color: "#ec4899",
    lightColor: "#f472b6",
    doorway: { x: -3.1, y: 0, z: 0 },
    floorColor: "#1e1b2e",
  },
  FINANCE: {
    id: "FINANCE",
    name: "Finance & Ops",
    center: { x: 0, y: 0, z: 4.2 },
    size: [5.5, 3.8],
    color: "#10b981",
    lightColor: "#34d399",
    doorway: { x: 0, y: 0, z: 2.3 },
    floorColor: "#1e293b",
  },
};

export interface EmployeeInfo {
  id: string;
  name: string;
  role: string;
  department: AgentType;
  deskPos: Position3D;
  deskRotation: number; // Y rotation in radians
  doorwayPos: Position3D;
  boardroomPos: Position3D;
  boardroomRotation: number;
  clothingColor: string;
  roleType: "CEO" | "DEV" | "MKT" | "FIN";
}

export const EMPLOYEES: EmployeeInfo[] = [
  {
    id: "emp-ceo",
    name: "Alex Vance",
    role: "Chief Executive Officer",
    department: "CEO",
    deskPos: { x: 0, y: 0, z: -5.42 },
    deskRotation: 0, // facing South (+Z) directly toward executive desk & primary monitor screen
    doorwayPos: { x: 0, y: 0, z: -2.3 },
    boardroomPos: { x: 0, y: 0, z: -1.3 },
    boardroomRotation: 0,
    clothingColor: "#f59e0b",
    roleType: "CEO",
  },
  {
    id: "emp-dev-1",
    name: "Dev Lead",
    role: "Lead Systems Architect",
    department: "DEVELOPMENT",
    deskPos: { x: 5.23, y: 0, z: -1.05 },
    deskRotation: Math.PI / 2, // facing East (+X) directly toward Dev 1 desk & monitor
    doorwayPos: { x: 3.1, y: 0, z: 0 },
    boardroomPos: { x: 1.4, y: 0, z: -0.6 },
    boardroomRotation: -Math.PI / 3,
    clothingColor: "#06b6d4",
    roleType: "DEV",
  },
  {
    id: "emp-dev-2",
    name: "Backend Eng",
    role: "Core Backend Engineer",
    department: "DEVELOPMENT",
    deskPos: { x: 5.23, y: 0, z: 1.05 },
    deskRotation: Math.PI / 2, // facing East (+X) directly toward Dev 2 desk & monitor
    doorwayPos: { x: 3.1, y: 0, z: 0 },
    boardroomPos: { x: 1.4, y: 0, z: 0.6 },
    boardroomRotation: (-2 * Math.PI) / 3,
    clothingColor: "#0891b2",
    roleType: "DEV",
  },
  {
    id: "emp-mkt-1",
    name: "Marketing Lead",
    role: "Head of Growth & Brand",
    department: "MARKETING",
    deskPos: { x: -5.23, y: 0, z: -1.05 },
    deskRotation: -Math.PI / 2, // facing West (-X) directly toward Mkt 1 desk & monitor
    doorwayPos: { x: -3.1, y: 0, z: 0 },
    boardroomPos: { x: -1.4, y: 0, z: -0.6 },
    boardroomRotation: Math.PI / 3,
    clothingColor: "#ec4899",
    roleType: "MKT",
  },
  {
    id: "emp-mkt-2",
    name: "Growth Lead",
    role: "Performance Marketing",
    department: "MARKETING",
    deskPos: { x: -5.23, y: 0, z: 1.05 },
    deskRotation: -Math.PI / 2, // facing West (-X) directly toward Mkt 2 desk & monitor
    doorwayPos: { x: -3.1, y: 0, z: 0 },
    boardroomPos: { x: -1.4, y: 0, z: 0.6 },
    boardroomRotation: (2 * Math.PI) / 3,
    clothingColor: "#db2777",
    roleType: "MKT",
  },
  {
    id: "emp-fin-1",
    name: "Finance Dir",
    role: "VP of Finance",
    department: "FINANCE",
    deskPos: { x: -1.8, y: 0, z: 5.42 },
    deskRotation: Math.PI, // facing North (-Z) directly toward Fin 1 desk & monitor
    doorwayPos: { x: 0, y: 0, z: 2.3 },
    boardroomPos: { x: -0.8, y: 0, z: 1.3 },
    boardroomRotation: Math.PI,
    clothingColor: "#10b981",
    roleType: "FIN",
  },
  {
    id: "emp-fin-2",
    name: "Controller",
    role: "Financial Controller",
    department: "FINANCE",
    deskPos: { x: 1.8, y: 0, z: 5.42 },
    deskRotation: Math.PI, // facing North (-Z) directly toward Fin 2 desk & monitor
    doorwayPos: { x: 0, y: 0, z: 2.3 },
    boardroomPos: { x: 0.8, y: 0, z: 1.3 },
    boardroomRotation: Math.PI,
    clothingColor: "#059669",
    roleType: "FIN",
  },
];
