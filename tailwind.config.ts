import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./src/pages/**/*.{ts,tsx}",
    "./src/components/**/*.{ts,tsx}",
    "./src/app/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        warm: "#FFF8F0",
        primary: {
          DEFAULT: "#E8642C",
          dark: "#C94F1F",
          soft: "#FFE7D8",
        },
        navy: "#1A1A2E",
        muted: "#6B7280",
        softYellow: "#FFD966",
        softBlue: "#B8E6F0",
        softRose: "#FFD4D4",
        softGreen: "#D4F0D4",
        border: "hsl(var(--border))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        destructive: "hsl(var(--destructive))",
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      borderRadius: {
        xl: "1rem",
        "2xl": "1.25rem",
        "3xl": "1.5rem",
      },
      boxShadow: {
        soft: "0 18px 50px rgba(26, 26, 46, 0.08)",
        lift: "0 24px 70px rgba(26, 26, 46, 0.12)",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
};

export default config;
