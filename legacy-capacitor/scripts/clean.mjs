import fs from "node:fs";
import path from "node:path";

const targets = [
  "node_modules",
  "web",
  "android/app/build",
  "android/.gradle",
  ".cache",
];

for (const target of targets) {
  const fullPath = path.resolve(process.cwd(), target);
  if (fs.existsSync(fullPath)) {
    fs.rmSync(fullPath, { recursive: true, force: true });
    console.log(`Removed ${target}`);
  }
}

console.log("Android client cleanup done.");
