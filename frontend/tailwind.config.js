/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0f5ff',
          100: '#dbe6fe',
          200: '#bdd0fd',
          300: '#93b1fb',
          400: '#6389f7',
          500: '#3b5cdb',
          600: '#2f47b8',
          700: '#263a94',
          800: '#1f2f76',
          900: '#1a275e',
        },
        ink: {
          950: '#0b0f19',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      boxShadow: {
        card: '0 1px 2px 0 rgba(15, 23, 42, 0.04), 0 1px 3px 0 rgba(15, 23, 42, 0.06)',
      },
    },
  },
  plugins: [],
}
