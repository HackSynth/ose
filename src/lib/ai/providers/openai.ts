import OpenAI from "openai";
import type { ChatCompletionMessageParam } from "openai/resources/chat/completions";
import type { AIConfig, AIProvider, CompletionParams } from "@/lib/ai/types";
import { getSanitizedEndpoint } from "@/lib/ai/utils";

const defaultBaseUrl = "https://api.openai.com/v1";
const defaultModel = "gpt-4o-mini";

function buildMessages(params: CompletionParams): ChatCompletionMessageParam[] {
  if (params.messages?.length) {
    return [
      { role: "system", content: params.systemPrompt },
      ...params.messages.map((message) => ({ role: message.role, content: message.content }) as ChatCompletionMessageParam),
    ];
  }
  return [
    { role: "system", content: params.systemPrompt },
    { role: "user", content: params.userMessage },
  ];
}

export function createOpenAIProvider(config: AIConfig): AIProvider {
  const apiKey = config.apiKey;
  const model = config.model || defaultModel;
  const baseUrl = config.baseUrl;

  function getClient() {
    if (!apiKey) throw new Error("未配置 OpenAI API Key");
    return new OpenAI({ apiKey, baseURL: baseUrl || undefined });
  }

  return {
    name: "OpenAI",
    getInfo() {
      return {
        name: "OpenAI",
        model,
        endpoint: getSanitizedEndpoint(baseUrl, defaultBaseUrl),
      };
    },
    async createCompletion(params) {
      const response = await getClient().chat.completions.create({
        model,
        max_tokens: params.maxTokens ?? 1200,
        temperature: params.temperature ?? 0.3,
        messages: buildMessages(params),
      });
      return response.choices[0]?.message.content ?? "";
    },
    async *streamCompletion(params) {
      const stream = await getClient().chat.completions.create({
        model,
        max_tokens: params.maxTokens ?? 1200,
        temperature: params.temperature ?? 0.3,
        messages: buildMessages(params),
        stream: true,
      });
      try {
        for await (const chunk of stream) {
          const text = chunk.choices[0]?.delta.content;
          if (text) yield text;
        }
      } finally {
        try {
          stream.controller?.abort?.();
        } catch {
          // ignore
        }
      }
    },
  };
}
