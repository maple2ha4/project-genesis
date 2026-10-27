import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "e2e",
  fullyParallel: false,
  workers: 1,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: "http://127.0.0.1:8085",
    launchOptions: {
      executablePath: process.env.PRISM_CHROMIUM || "/usr/bin/chromium",
      args: ["--no-sandbox"],
    },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: [{
    command:
      'PORT=8085 PRISM_DATABASE_URL="jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1" bash ../scripts/run.sh',
    url: "http://127.0.0.1:8085/api/health",
    reuseExistingServer: false,
    timeout: 60000,
  },{command:'PORT=8087 PRISM_USERNAME=browser-owner PRISM_PASSWORD=browser-test-only-123 PRISM_DATABASE_URL="jdbc:h2:mem:secure-e2e;DB_CLOSE_DELAY=-1" bash ../scripts/run.sh',url:"http://127.0.0.1:8087/api/health",reuseExistingServer:false,timeout:60000}],
});
