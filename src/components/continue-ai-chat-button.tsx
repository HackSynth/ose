'use client';

import { MessageCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { continueAIChat, type AIChatClientMessage } from '@/lib/ai-chat-client';

export function ContinueAIChatButton({
  title,
  messages,
}: {
  title: string;
  messages: AIChatClientMessage[];
}) {
  return (
    <Button type="button" variant="secondary" onClick={() => continueAIChat(title, messages)}>
      <MessageCircle className="h-4 w-4" />
      继续对话
    </Button>
  );
}
