"use client";

import { memo } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { cn } from "@/lib/utils";

function MarkdownRendererImpl({ content, className }: { content: string; className?: string }) {
  return (
    <div className={cn("prose prose-sm max-w-none text-navy prose-headings:text-navy prose-strong:text-navy prose-code:rounded prose-code:bg-warm prose-code:px-1 prose-pre:rounded-2xl prose-pre:bg-warm prose-li:my-1", className)}>
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
    </div>
  );
}

export const MarkdownRenderer = memo(MarkdownRendererImpl);
