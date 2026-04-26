export type AIChatClientMessage = { role: 'user' | 'assistant'; content: string };

export const OSE_AI_CONTINUE_EVENT = 'ose-ai-continue-chat';

export function continueAIChat(title: string, messages: AIChatClientMessage[]) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(
    new CustomEvent(OSE_AI_CONTINUE_EVENT, {
      detail: { title, messages },
    })
  );
}
