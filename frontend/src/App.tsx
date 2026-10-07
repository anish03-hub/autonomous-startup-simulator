import { useState } from "react";
import { useSim } from "./store/simulationStore";
import { useEventStream } from "./hooks/useEventStream";
import LandingPage from "./pages/LandingPage";
import SimulatorPage from "./pages/SimulatorPage";

export default function App() {
  const startup = useSim((s) => s.startup);
  const onEvent = useSim((s) => s.onEvent);
  const [entered, setEntered] = useState(false);

  // One SSE connection for the whole app, active once a startup exists.
  const { connected } = useEventStream(startup ? startup.id : null, onEvent);

  if (!startup || !entered) {
    return <LandingPage onReady={() => setEntered(true)} />;
  }
  return <SimulatorPage sseConnected={connected} />;
}
