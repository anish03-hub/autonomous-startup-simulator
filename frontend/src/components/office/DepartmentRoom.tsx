import type { Agent, AgentMessage, AgentTask, AgentType } from "../../types";
import { DEPT } from "../../lib/theme";
import AgentAvatar from "./AgentAvatar";

interface Props {
  type: AgentType;
  agent?: Agent;
  task?: AgentTask;
  lastMessage?: AgentMessage;
  central?: boolean;
  onClick: () => void;
}

/**
 * One isometric room per department. The floor tilts with the plane; contents
 * are counter-rotated (.iso-upright) so avatars and labels stay readable.
 */
export default function DepartmentRoom({
  type,
  agent,
  task,
  lastMessage,
  central,
  onClick,
}: Props) {
  const t = DEPT[type];
  const progress = task?.progress ?? 0;

  return (
    <button
      onClick={onClick}
      className={`iso-room group relative text-left rounded-xl border bg-gradient-to-br ${t.room} ${
        central ? "ring-2 ring-ceo/50 " + t.glow : ""
      }`}
      style={{ width: central ? 240 : 210, height: central ? 240 : 210 }}
    >
      <div className="room-floor absolute inset-0 rounded-xl" />
      <div className="iso-upright absolute inset-0 p-3 flex flex-col">
        <div className="flex items-center justify-between">
          <span className={`text-xs font-bold uppercase tracking-wider ${t.accent}`}>
            {t.label}
          </span>
          <span className="text-lg drop-shadow">{t.emoji}</span>
        </div>

        <div className="flex-1 grid place-items-center">
          {agent ? (
            <AgentAvatar agent={agent} />
          ) : (
            <div className="text-slate-500 text-xs">Empty desk</div>
          )}
        </div>

        {task && (
          <div className="space-y-1">
            <div className="text-[10px] text-slate-300 truncate">{task.title}</div>
            <div className="h-1.5 rounded-full bg-black/40 overflow-hidden">
              <div
                className="h-full rounded-full transition-all duration-500"
                style={{ width: `${progress}%`, backgroundColor: t.hex }}
              />
            </div>
          </div>
        )}

        {lastMessage && (
          <div className="mt-2 rounded-lg bg-black/30 px-2 py-1 text-[10px] text-slate-300 line-clamp-2 opacity-0 group-hover:opacity-100 transition">
            “{lastMessage.content}”
          </div>
        )}
      </div>
    </button>
  );
}
