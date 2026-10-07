import { useEffect, useRef, useState } from "react";
import { streamUrl } from "../services/api";
import type { StartupEvent } from "../types";

/**
 * Subscribes to the backend SSE stream for a startup and invokes `onEvent` for
 * every `startup-event`. Reconnects are handled by the browser's EventSource.
 */
export function useEventStream(
  startupId: number | null,
  onEvent: (event: StartupEvent) => void
) {
  const [connected, setConnected] = useState(false);
  const handlerRef = useRef(onEvent);
  handlerRef.current = onEvent;

  useEffect(() => {
    if (startupId == null) return;

    const es = new EventSource(streamUrl(startupId));

    es.addEventListener("connected", () => setConnected(true));
    es.addEventListener("startup-event", (e) => {
      try {
        const data = JSON.parse((e as MessageEvent).data) as StartupEvent;
        handlerRef.current(data);
      } catch {
        /* ignore malformed frame */
      }
    });
    es.onerror = () => setConnected(false);

    return () => es.close();
  }, [startupId]);

  return { connected };
}
