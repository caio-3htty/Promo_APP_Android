import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.prumo.androidclient",
  appName: "Prumo Android Client",
  webDir: "web",
  android: {
    allowMixedContent: true,
  },
};

export default config;
