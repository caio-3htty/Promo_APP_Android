import fs from "fs";
import path from "path";
import { spawnSync } from "child_process";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, "..");
const androidDir = path.resolve(projectRoot, "android");

if (fs.existsSync(androidDir)) {
  console.log("Android platform already exists.");
  process.exit(0);
}

console.log("Android platform not found. Running `npx cap add android`...");
const major = Number(process.versions.node.split(".")[0] || "0");
const commandArgs =
  major >= 20
    ? ["cap", "add", "android"]
    : ["-y", "node@20", "node_modules/@capacitor/cli/bin/capacitor", "add", "android"];

const result = spawnSync("npx", commandArgs, {
  cwd: projectRoot,
  stdio: "inherit",
  shell: process.platform === "win32",
});

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}
