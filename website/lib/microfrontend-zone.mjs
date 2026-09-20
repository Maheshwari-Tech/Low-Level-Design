function normalizedPath(value) {
  const path = String(value || "").trim().replace(/^\/+|\/+$/g, "");
  return path ? `/${path}` : "";
}

export function lldZoneConfig(environment = process.env) {
  return {
    basePath: normalizedPath(environment.LLD_BASE_PATH),
    assetPrefix: normalizedPath(environment.LLD_ASSET_PREFIX) || undefined,
  };
}
