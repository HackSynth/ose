import Anthropic from "@anthropic-ai/sdk";
import type { AIConfig, AIProvider, CompletionParams } from "@/lib/ai/types";
import { getSanitizedEndpoint } from "@/lib/ai/utils";

const defaultBaseUrl = "https://api.anthropic.com";
const defaultModel = "claude-sonnet-4-5-20250929";

function buildMessages(params: CompletionParams): Anthropic.MessageParam[] {
  if (params.messages?.length) {
    return params.messages.map((message) => ({ role: message.role, content: message.content }));
  }
  return [{ role: "user", content: params.userMessage }];
}

export function createClaudeProvider(config: AIConfig): AIProvider {
  const apiKey = config.apiKey;
  const model = config.model || defaultModel;
  const baseUrl = config.baseUrl;

  function getClient() {
    if (!apiKey) throw new Error("未配置 Claude API Key");
    return new Anthropic({ apiKey, baseURL: baseUrl || undefined });
  }

  return {
    name: "Claude",
    getInfo() {
      return {
        name: "Claude",
        model,
        endpoint: getSanitizedEndpoint(baseUrl, defaultBaseUrl),
      };
    },
    async createCompletion(params) {
      const response = await getClient().messages.create({
        model,
        max_tokens: params.maxTokens ?? 1200,
        temperature: params.temperature ?? 0.3,
        system: params.systemPrompt,
        messages: buildMessages(params),
      });
      return response.content
        .filter((block): block is Extract<Anthropic.ContentBlock, { type: "text" }> => block.type === "text")
        .map((block) => block.text)
        .join("");
    },
    async *streamCompletion(params) {
      const stream = getClient().messages.stream({
        model,
        max_tokens: params.maxTokens ?? 1200,
        temperature: params.temperature ?? 0.3,
        system: params.systemPrompt,
        messages: buildMessages(params),
      });
      try {
        for await (const event of stream) {
          if (event.type === "content_block_delta" && event.delta.type === "text_delta") {
            yield event.delta.text;
          }
        }
      } finally {
        try {
          await stream.abort();
        } catch {
          // best-effort cleanup
        }
      }
    },
  };
}
