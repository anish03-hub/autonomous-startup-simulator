/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        // Surrounding UI: deep navy control-room palette.
        ink: {
          950: "#070b18",
          900: "#0b1120",
          850: "#0f172a",
          800: "#131c31",
          700: "#1e293b",
          600: "#334155",
        },
        // Department accent colors — coherent, distinct per room.
        ceo: "#f5b301",
        dev: "#38bdf8",
        marketing: "#f472b6",
        finance: "#34d399",
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
      },
      keyframes: {
        "pulse-ring": {
          "0%": { transform: "scale(0.8)", opacity: "0.7" },
          "80%,100%": { transform: "scale(1.8)", opacity: "0" },
        },
        bob: {
          "0%,100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-4px)" },
        },
        "fade-in-up": {
          "0%": { opacity: "0", transform: "translateY(8px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
      },
      animation: {
        "pulse-ring": "pulse-ring 1.8s cubic-bezier(0.4,0,0.6,1) infinite",
        bob: "bob 2.4s ease-in-out infinite",
        "fade-in-up": "fade-in-up 0.35s ease-out",
      },
    },
  },
  plugins: [],
};
