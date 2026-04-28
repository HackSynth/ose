'use client';

import { memo } from 'react';
import ReactMarkdown, { type Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { cn } from '@/lib/utils';

// Custom <img> renderer: 51CTO and other exam-source CDNs reject hotlinked
// requests that include a localhost referrer. `referrerPolicy="no-referrer"`
// strips it and lets the image load. `loading="lazy"` keeps long case
// scenarios from blocking the first paint.
const components: Components = {
  img: ({ src, alt, ...rest }) => (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={typeof src === 'string' ? src : undefined}
      alt={alt ?? ''}
      referrerPolicy="no-referrer"
      loading="lazy"
      {...rest}
    />
  ),
};

function readHtmlAttribute(tag: string, name: string) {
  const match = new RegExp(`${name}\\s*=\\s*(?:"([^"]*)"|'([^']*)'|([^\\s>]+))`, 'i').exec(tag);
  return (match?.[1] ?? match?.[2] ?? match?.[3] ?? '').replaceAll('&amp;', '&');
}

function normalizeMarkdown(content: string) {
  return content.replace(/<br\s*\/?>/gi, '\n').replace(/<img\b[^>]*>/gi, (tag) => {
    const src = readHtmlAttribute(tag, 'src');
    if (!src) return '';
    const alt = readHtmlAttribute(tag, 'alt').replaceAll('[', '').replaceAll(']', '');
    return `![${alt}](${src})`;
  });
}

function MarkdownRendererImpl({ content, className }: { content: string; className?: string }) {
  return (
    <div
      className={cn(
        'prose prose-sm max-w-none text-navy prose-headings:text-navy prose-strong:text-navy prose-code:rounded prose-code:bg-warm prose-code:px-1 prose-pre:rounded-2xl prose-pre:bg-warm prose-li:my-1',
        className
      )}
    >
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {normalizeMarkdown(content)}
      </ReactMarkdown>
    </div>
  );
}

export const MarkdownRenderer = memo(MarkdownRendererImpl);
