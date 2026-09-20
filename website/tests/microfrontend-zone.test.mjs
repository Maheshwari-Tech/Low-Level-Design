import assert from "node:assert/strict";
import test from "node:test";

import { lldZoneConfig } from "../lib/microfrontend-zone.mjs";
import { lldPublicAsset } from "../app/lib/public-asset.mjs";

test("keeps the standalone LLD website at root", () => {
  assert.deepEqual(lldZoneConfig({}), {basePath: "", assetPrefix: undefined});
  assert.equal(lldPublicAsset("/lld-content.json", ""), "/lld-content.json");
});

test("builds the LLD zone under the candidate learning path", () => {
  assert.deepEqual(lldZoneConfig({
    LLD_BASE_PATH: "candidate/learn/lld/",
    LLD_ASSET_PREFIX: "lld-static/",
  }), {
    basePath: "/candidate/learn/lld",
    assetPrefix: "/lld-static",
  });
  assert.equal(
    lldPublicAsset("repository-content/lld-atlas.json", "/candidate/learn/lld/"),
    "/candidate/learn/lld/repository-content/lld-atlas.json",
  );
});
