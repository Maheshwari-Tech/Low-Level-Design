function normalizedBasePath(value) {
  const path = String(value || "").trim().replace(/^\/+|\/+$/g, "");
  return path ? `/${path}` : "";
}

export function lldPublicAsset(path, configuredBasePath = process.env.NEXT_PUBLIC_LLD_BASE_PATH) {
  const normalizedAsset = `/${String(path || "").replace(/^\/+/, "")}`;
  return `${normalizedBasePath(configuredBasePath)}${normalizedAsset}`;
}
