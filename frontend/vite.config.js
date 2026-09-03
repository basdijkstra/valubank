import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Explicitly pinned to Vite's default. The backend services (Accounts
    // Service and Payments Service) are configured to allow CORS requests
    // from http://localhost:5173, so keep this port stable.
    port: 5173
  }
})
