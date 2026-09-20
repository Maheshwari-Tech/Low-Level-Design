import type { NextConfig } from "next";
import { lldZoneConfig } from "./lib/microfrontend-zone.mjs";

const zone = lldZoneConfig();

const nextConfig: NextConfig = {
  basePath: zone.basePath,
  assetPrefix: zone.assetPrefix,
  turbopack: { root: process.cwd() },
};

export default nextConfig;
