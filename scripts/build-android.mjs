import path from "path";
import { spawnSync } from "child_process";
import fs from "fs";
import os from "os";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, "..");
const androidDir = path.resolve(projectRoot, "android");
const gradleCmd = process.platform === "win32" ? "gradlew.bat" : "./gradlew";

function resolveJavaHome() {
  if (process.env.JAVA_HOME && fs.existsSync(process.env.JAVA_HOME)) {
    return process.env.JAVA_HOME;
  }

  const jdkRoot = path.join(os.homedir(), ".jdks");
  if (!fs.existsSync(jdkRoot)) {
    return null;
  }

  const candidates = fs
    .readdirSync(jdkRoot, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => path.join(jdkRoot, entry.name))
    .filter((dir) => path.basename(dir).startsWith("jdk-21"));

  return candidates[0] ?? null;
}

function resolveAndroidSdkRoot() {
  if (process.env.ANDROID_SDK_ROOT && fs.existsSync(process.env.ANDROID_SDK_ROOT)) {
    return process.env.ANDROID_SDK_ROOT;
  }

  const fallback = path.join(os.homedir(), "AppData", "Local", "Android", "Sdk");
  return fs.existsSync(fallback) ? fallback : null;
}

const javaHome = resolveJavaHome();
const androidSdkRoot = resolveAndroidSdkRoot();

const env = { ...process.env };
if (javaHome) {
  env.JAVA_HOME = javaHome;
  env.PATH = `${path.join(javaHome, "bin")}${path.delimiter}${env.PATH ?? ""}`;
}

if (androidSdkRoot) {
  env.ANDROID_SDK_ROOT = androidSdkRoot;
  env.ANDROID_HOME = androidSdkRoot;
}

const result = spawnSync(gradleCmd, ["assembleDebug"], {
  cwd: androidDir,
  env,
  stdio: "inherit",
  shell: process.platform === "win32",
});

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}
