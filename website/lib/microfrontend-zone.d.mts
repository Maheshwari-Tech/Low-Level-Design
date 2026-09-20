export type LldZoneConfig = {basePath: string; assetPrefix?: string};

export function lldZoneConfig(environment?: Record<string, string | undefined>): LldZoneConfig;
