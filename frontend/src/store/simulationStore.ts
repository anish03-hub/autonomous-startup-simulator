import { create } from "zustand";
import { api } from "../services/api";
import type {
  Agent,
  AgentMessage,
  AgentTask,
  Blueprint,
  Debate,
  ExecutionState,
  Plans,
  Startup,
  StartupEvent,
} from "../types";

interface SimState {
  startup: Startup | null;
  agents: Agent[];
  tasks: AgentTask[];
  messages: AgentMessage[];
  debates: Debate[];
  events: StartupEvent[];
  blueprint: Blueprint | null;
  plans: Plans | null;
  executionState: ExecutionState | null;
  loading: boolean;
  error: string | null;

  create: (idea: string, name?: string) => Promise<Startup>;
  load: (id: number) => Promise<void>;
  startSimulation: (speed?: number) => Promise<void>;
  retryCeoAnalysis: () => Promise<void>;
  retryDepartmentAnalysis: (dept: "developer" | "marketing" | "finance") => Promise<void>;
  onEvent: (event: StartupEvent) => void;
  refresh: () => Promise<void>;
  loadBlueprint: () => Promise<void>;
  loadPlans: () => Promise<void>;
  loadExecutionState: () => Promise<void>;
  startExecution: () => Promise<void>;
  pauseExecution: () => Promise<void>;
  resumeExecution: () => Promise<void>;
  stepExecution: () => Promise<void>;
  resolveTaskBlocker: (taskId: number) => Promise<void>;
  reset: () => void;
}

export const useSim = create<SimState>((set, get) => ({
  startup: null,
  agents: [],
  tasks: [],
  messages: [],
  debates: [],
  events: [],
  blueprint: null,
  plans: null,
  executionState: null,
  loading: false,
  error: null,

  async create(idea, name) {
    set({ loading: true, error: null });
    try {
      const startup = await api.createStartup(idea, name);
      set({ startup, loading: false });
      await get().load(startup.id);
      return startup;
    } catch (e) {
      set({ loading: false, error: (e as Error).message });
      throw e;
    }
  },

  async load(id) {
    const [startup, agents, tasks, messages, debates, events, plans] = await Promise.all([
      api.getStartup(id),
      api.agents(id),
      api.tasks(id),
      api.messages(id),
      api.debates(id),
      api.events(id),
      api.plans(id),
    ]);
    set({ startup, agents, tasks, messages, debates, events, plans });
    await get().loadExecutionState();
  },

  async startSimulation(speed = 1.0) {
    const s = get().startup;
    if (!s) return;
    await api.simulate(s.id, speed);
  },

  async retryCeoAnalysis() {
    const s = get().startup;
    if (!s) return;
    await api.retryCeoAnalysis(s.id);
    void get().refresh();
  },

  async retryDepartmentAnalysis(dept) {
    const s = get().startup;
    if (!s) return;
    await api.retryDepartmentAnalysis(s.id, dept);
    void get().refresh();
  },

  onEvent(event) {
    set((st) => ({ events: [...st.events, event] }));
    void get().refresh();
    if (event.type === "BLUEPRINT_GENERATED") void get().loadBlueprint();
    if (event.type.startsWith("EXECUTION_") || event.type.startsWith("TASK_") || event.type === "METRIC_CHANGED") {
      void get().loadExecutionState();
    }
  },

  async refresh() {
    const s = get().startup;
    if (!s) return;
    const [startup, agents, tasks, messages, debates, plans] = await Promise.all([
      api.getStartup(s.id),
      api.agents(s.id),
      api.tasks(s.id),
      api.messages(s.id),
      api.debates(s.id),
      api.plans(s.id),
    ]);
    set({ startup, agents, tasks, messages, debates, plans });
    void get().loadExecutionState();
  },

  async loadBlueprint() {
    const s = get().startup;
    if (!s) return;
    try {
      set({ blueprint: await api.blueprint(s.id) });
    } catch {
      /* blueprint not ready yet */
    }
  },

  async loadPlans() {
    const s = get().startup;
    if (!s) return;
    try {
      set({ plans: await api.plans(s.id) });
    } catch {
      /* plans not ready yet */
    }
  },

  async loadExecutionState() {
    const s = get().startup;
    if (!s) return;
    try {
      const executionState = await api.getExecutionState(s.id);
      set({ executionState });
    } catch {
      /* execution state not initialized yet */
    }
  },

  async startExecution() {
    const s = get().startup;
    if (!s) return;
    const executionState = await api.startExecution(s.id);
    set({ executionState });
    void get().refresh();
  },

  async pauseExecution() {
    const s = get().startup;
    if (!s) return;
    const executionState = await api.pauseExecution(s.id);
    set({ executionState });
  },

  async resumeExecution() {
    const s = get().startup;
    if (!s) return;
    const executionState = await api.resumeExecution(s.id);
    set({ executionState });
  },

  async stepExecution() {
    const s = get().startup;
    if (!s) return;
    const executionState = await api.stepExecution(s.id);
    set({ executionState });
    void get().refresh();
  },

  async resolveTaskBlocker(taskId: number) {
    const s = get().startup;
    if (!s) return;
    const executionState = await api.resolveTaskBlocker(s.id, taskId);
    set({ executionState });
    void get().refresh();
  },

  reset() {
    set({
      startup: null,
      agents: [],
      tasks: [],
      messages: [],
      debates: [],
      events: [],
      blueprint: null,
      plans: null,
      executionState: null,
      error: null,
    });
  },
}));
