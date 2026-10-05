import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The production build is written straight into the Spring Boot static folder,
// so one jar serves both the API and the website.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
