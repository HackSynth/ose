"use client";

import Image from "next/image";

type Figure =
  | string
  | {
      url?: string;
      src?: string;
      caption?: string;
      title?: string;
      description?: string;
      text?: string;
    };

function normalize(raw: unknown): Figure[] {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.filter((item) => item !== null && item !== undefined) as Figure[];
  if (typeof raw === "string") return [raw];
  if (typeof raw === "object") return [raw as Figure];
  return [];
}

function isImageUrl(value?: string) {
  if (!value) return false;
  return /\.(png|jpg|jpeg|gif|webp|svg)(\?|$)/i.test(value) || value.startsWith("data:image/");
}

function isDataUrl(value: string) {
  return value.startsWith("data:");
}

function renderImage(src: string, alt: string, key: number | string) {
  if (isDataUrl(src)) {
    // Next Image can't optimize data URLs; skip optimization only here.
    return <Image key={key} src={src} alt={alt} width={800} height={480} className="h-auto w-full rounded-xl object-contain" unoptimized />;
  }
  return <Image key={key} src={src} alt={alt} width={800} height={480} className="h-auto w-full rounded-xl object-contain" />;
}

export function CaseFigures({ figures }: { figures: unknown }) {
  const items = normalize(figures);
  if (!items.length) return null;
  return (
    <div className="mt-6 space-y-3 rounded-3xl border-2 border-dashed border-primary/30 bg-primary-soft/40 p-5">
      <p className="font-black text-navy">图表 / 附图</p>
      <div className="space-y-4">
        {items.map((item, index) => {
          if (typeof item === "string") {
            if (isImageUrl(item)) {
              return (
                <figure key={index} className="overflow-hidden rounded-2xl bg-white p-3">
                  {renderImage(item, `figure-${index + 1}`, index)}
                </figure>
              );
            }
            return (
              <p key={index} className="whitespace-pre-wrap font-semibold leading-7 text-muted">
                {item}
              </p>
            );
          }
          const src = item.url ?? item.src;
          const caption = item.caption ?? item.title;
          const description = item.description ?? item.text;
          return (
            <figure key={index} className="overflow-hidden rounded-2xl bg-white p-3">
              {src && isImageUrl(src) ? renderImage(src, caption ?? `figure-${index + 1}`, `img-${index}`) : null}
              {caption ? <figcaption className="mt-2 font-black text-navy">{caption}</figcaption> : null}
              {description ? <p className="mt-1 whitespace-pre-wrap font-semibold leading-7 text-muted">{description}</p> : null}
            </figure>
          );
        })}
      </div>
    </div>
  );
}
