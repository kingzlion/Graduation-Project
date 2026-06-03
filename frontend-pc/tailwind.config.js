/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        darkBg: '#050608',
        panelBg: '#0f1118',
        glassBg: 'rgba(13, 15, 19, 0.65)',
        glassBorder: 'rgba(255, 255, 255, 0.06)',
        neonCyan: '#00ecff',
        neonGold: '#f39c12',
        neonPurple: '#9b59b6',
        neonRed: '#ff4757',
        textMain: '#f3f4f6',
        textMuted: '#9ca3af'
      },
      fontFamily: {
        sans: ['Inter', 'Outfit', 'SF Pro SC', 'PingFang SC', 'sans-serif'],
      },
      boxShadow: {
        'glass': '0 20px 50px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05)',
        'cyan-glow': '0 0 20px rgba(0, 236, 255, 0.35)',
        'gold-glow': '0 0 20px rgba(243, 156, 18, 0.35)',
        'purple-glow': '0 0 25px rgba(155, 89, 182, 0.4)',
        'red-glow': '0 0 20px rgba(255, 71, 87, 0.35)'
      }
    },
  },
  plugins: [],
}
