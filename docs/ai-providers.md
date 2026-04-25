# AI Providers

OSE supports environment-level AI configuration and per-user AI settings from the profile page.

## Provider Priority

If `AI_PROVIDER` is empty, OSE attempts to detect a configured provider based on available API keys. A typical priority is Claude, OpenAI, Gemini, then custom compatible endpoints.

## Claude

```env
AI_PROVIDER=claude
ANTHROPIC_API_KEY=sk-ant-...
ANTHROPIC_MODEL=claude-sonnet-4-5-20250929
```

Use Claude for high-quality case analysis grading, explanations, and study planning. `ANTHROPIC_BASE_URL` can point to a trusted proxy.

## OpenAI

```env
AI_PROVIDER=openai
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
```

Use OpenAI for fast explanations, question generation, and chat. Set `OPENAI_BASE_URL` only when using a compatible gateway.

## Gemini

```env
AI_PROVIDER=gemini
GEMINI_API_KEY=...
GEMINI_MODEL=gemini-2.5-flash
```

Gemini is useful for cost-efficient generation and long-context tasks.

## Custom Endpoint

```env
AI_PROVIDER=custom
CUSTOM_API_KEY=local-key
CUSTOM_BASE_URL=http://localhost:11434/v1
CUSTOM_MODEL=llama3
```

Custom mode works with OpenAI-compatible APIs such as Ollama, DeepSeek, Qwen, vLLM, LM Studio, LocalAI, and some Azure OpenAI gateways.

## Per-user Settings

Users can configure provider, key, base URL, and model from the profile page. This is useful for shared deployments where each learner brings their own API key.

## Safety Notes

- Do not commit real API keys.
- Prefer HTTPS for remote custom endpoints.
- Rotate keys after testing public demos.
- Use provider rate limits to control cost.
