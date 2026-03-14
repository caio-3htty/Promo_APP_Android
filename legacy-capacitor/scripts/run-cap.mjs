import { spawnSync } from "child_process";

const args = process.argv.slice(2);
const major = Number(process.versions.node.split(".")[0] || "0");

const commandArgs =
  major >= 20
    ? ["cap", ...args]
    : ["-y", "node@20", "node_modules/@capacitor/cli/bin/capacitor", ...args];

const result = spawnSync("npx", commandArgs, {
  stdio: "inherit",
  shell: process.platform === "win32",
});

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}
