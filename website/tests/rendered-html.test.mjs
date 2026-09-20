import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import test from "node:test";

const templateRoot = new URL("../", import.meta.url);

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request("http://localhost/", {
      headers: { accept: "text/html" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );
}

test("server-renders the LLD Atlas homepage", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>Learn Low-Level Design — Questions, Patterns &amp; Code · LLD Atlas<\/title>/);
  assert.match(html, /Design systems from/);
  assert.match(html, /Eight Senior \/ Staff interview packs/);
  assert.match(html, /One hour\. Question, reasoning, proof\./);
  assert.match(html, /60-minute interview solutions/);
  assert.match(html, /Interview guide/);
  assert.match(html, /interview variations/);
  assert.match(html, /featured interview variations/);
  assert.match(html, /47(?:<!-- -->)? source-backed variations/);
  assert.match(html, /Java \+ Python/);
  assert.match(html, /Python files/);
  assert.match(html, /Complete problem bank/);
  assert.match(html, /Order Management System/);
  assert.match(html, /Read full explanation/);
  assert.match(html, /Every card opens a complete design explanation/);
  assert.equal((html.match(/class="problem-card"/g) ?? []).length, 92);
  assert.match(html, /Principles, by design pressure/);
  assert.match(html, /The complete GoF catalog/);
  assert.match(html, /Single Responsibility Principle/);
  assert.match(html, /Chain of Responsibility/);
  assert.match(html, /Java Multithreading &amp; Concurrency for LLD Interviews/);
  assert.match(html, /Open guide/);
  assert.match(html, /Exact provenance behind every variation/);
  assert.match(html, /The solution is merged\. The evidence stays traceable/);
  assert.match(html, /LLD Atlas canonical repository/);
  assert.match(html, /awesome-low-level-design/);
  assert.match(html, /low-level-design-primer/);
  assert.match(html, /kumaransg\/LLD/);
  assert.match(html, /fc26e403/);
  assert.match(html, /49fe9f2f/);
  assert.match(html, /1698cc6f/);
  assert.match(html, /Search this repository/);
  assert.doesNotMatch(html, /Your site is taking shape|react-loading-skeleton/);
});

test("keeps generated content and social metadata wired", async () => {
  const [content, css, page, explorer, solutionExplorer, catalogExplorer, fileTree, learningLinks, sourceLibrary, layout, packageJson] = await Promise.all([
    readFile(new URL("../public/lld-content.json", import.meta.url), "utf8"),
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/components/KnowledgeExplorer.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/components/SolutionExplorer.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/components/CatalogExplorer.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/components/FileTree.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/components/learningLinks.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/components/SourceLibrary.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
    readFile(new URL("../package.json", import.meta.url), "utf8"),
  ]);

  const parsed = JSON.parse(content);
  assert.equal(parsed.schemaVersion, 4);
  assert.equal(parsed.catalog.length, 92);
  assert.ok(parsed.catalog.every((item) => item.readme.length > 0 && Array.isArray(item.files)));
  assert.equal(parsed.principles.length, 6);
  assert.equal(parsed.patterns.length, 23);
  assert.ok(parsed.lessons.length >= 6);
  const interviewPlaybook = parsed.lessons.find((item) => item.slug === "senior_staff_lld_interview");
  assert.ok(interviewPlaybook);
  assert.match(interviewPlaybook.readme, /60-Minute Senior\/Staff LLD Interview Playbook/);
  assert.match(interviewPlaybook.readme, /Applying SOLID without ceremony/);
  assert.match(interviewPlaybook.readme, /Senior versus Staff depth/);
  const javaConcurrencyGuide = parsed.lessons.find((item) => item.slug === "java_concurrency_lld_interviews");
  assert.ok(javaConcurrencyGuide);
  assert.match(javaConcurrencyGuide.readme, /Java Multithreading and Concurrency for LLD Interviews/);
  assert.match(javaConcurrencyGuide.readme, /ExecutorService and thread pools/);
  assert.match(javaConcurrencyGuide.readme, /BlockingQueue and producer-consumer/);
  assert.match(javaConcurrencyGuide.readme, /Semaphore/);
  assert.match(javaConcurrencyGuide.readme, /synchronized/);
  assert.match(javaConcurrencyGuide.readme, /Testing concurrent code/);
  assert.equal(javaConcurrencyGuide.javaFileCount, 1);
  assert.ok(javaConcurrencyGuide.files.some((file) => file.path.endsWith("ConcurrencyInterviewLab.java")));
  assert.match(javaConcurrencyGuide.files[0].code, /ExecutorService/);
  assert.match(javaConcurrencyGuide.files[0].code, /ArrayBlockingQueue/);
  assert.match(javaConcurrencyGuide.files[0].code, /Semaphore/);
  const featuredInterviewVariations = {
    order_processing_system: 5,
    inventory_reservation_service: 9,
    coupon_promotion_engine: 6,
    notification_framework: 7,
    payment_processing_service: 6,
    warehouse_fulfilment_domain: 5,
    rate_limiter: 4,
    product_catalog_service: 5,
  };
  let totalVariations = 0;
  for (const [slug, expectedVariations] of Object.entries(featuredInterviewVariations)) {
    const problem = parsed.catalog.find((item) => item.slug === slug);
    assert.ok(problem, `missing featured interview problem: ${slug}`);
    assert.match(problem.readme, /60-Minute Senior\/Staff Interview Guide/);
    assert.match(problem.readme, /Candidate-facing question/);
    assert.match(problem.readme, /Expected solution and design-principle reasoning/);
    assert.match(problem.readme, /Interview variations and expected solutions/);
    assert.match(problem.readme, /#### Variation 1/);
    assert.match(problem.readme, /One-hour Python coding scope/);
    const variationCount = (problem.readme.match(/^#### Variation \d+\b/gm) ?? []).length;
    assert.equal(variationCount, expectedVariations, `variation coverage drifted: ${slug}`);
    assert.ok((problem.readme.match(/Expected Senior solution/g) ?? []).length >= variationCount);
    assert.ok((problem.readme.match(/Staff(?:-level)? extension/g) ?? []).length >= variationCount);
    assert.ok((problem.readme.match(/github\.com\//g) ?? []).length >= variationCount);
    assert.doesNotMatch(problem.readme, /### Reference material and variations/);
    assert.ok(problem.pythonFileCount >= 2, `missing Python solution files: ${slug}`);
    assert.ok(problem.files.some((file) => file.path === "python/solution.py"));
    assert.ok(problem.files.some((file) => file.path === "python/test_solution.py"));
    totalVariations += variationCount;
  }
  assert.equal(totalVariations, 47);
  assert.equal(parsed.repositories.length, 4);
  assert.ok([...parsed.lessons, ...parsed.principles, ...parsed.patterns].every((item) => item.readme.length > 0 && Array.isArray(item.files)));
  assert.ok([...parsed.catalog, ...parsed.lessons, ...parsed.principles, ...parsed.patterns].reduce((sum, item) => sum + item.files.length, 0) >= 500);
  assert.ok(new Set(parsed.catalog.map((item) => item.category)).size >= 6);
  assert.ok(parsed.catalog.every((item) => Number.isInteger(item.pythonFileCount)));
  assert.match(page, /SolutionExplorer/);
  assert.match(page, /KnowledgeExplorer/);
  assert.match(page, /SourceLibrary/);
  assert.match(page, /href="#principles"/);
  assert.match(page, /href="#patterns"/);
  assert.match(page, /href="#sources"/);
  assert.match(explorer, /#\$\{kind\}\/\$\{item\.slug\}/);
  assert.match(explorer, /aria-haspopup="dialog"/);
  assert.match(explorer, /<FileTree/);
  assert.match(solutionExplorer, /selectSourceLanguage/);
  assert.match(solutionExplorer, />Python <span>/);
  assert.match(solutionExplorer, /<FileTree/);
  assert.match(catalogExplorer, /sourceLanguage/);
  assert.match(catalogExplorer, />Python<\/button>/);
  assert.match(catalogExplorer, /<FileTree/);
  assert.match(fileTree, /function buildTree/);
  assert.match(fileTree, /role="tree"/);
  assert.match(fileTree, /source-tree-folder/);
  assert.match(learningLinks, /JAVA_MULTITHREADING_INTERVIEW_GUIDE\.md/);
  assert.match(learningLinks, /#lesson\/java_concurrency_lld_interviews/);
  assert.match(sourceLibrary, /const PAGE_SIZE = 80/);
  assert.match(sourceLibrary, /repository-content\/\$\{encodeURIComponent\(id\)\}\.json/);
  assert.match(sourceLibrary, /useState\(false\)/);
  assert.match(sourceLibrary, /#source\/\$\{repository\.id\}/);
  assert.match(sourceLibrary, /View exact source/);
  assert.match(sourceLibrary, /resolveRepositoryMarkdownLink/);
  assert.match(sourceLibrary, /Concurrency\/JAVA_MULTITHREADING_INTERVIEW_GUIDE\.md/);
  assert.match(sourceLibrary, /#lesson\/java_concurrency_lld_interviews/);
  assert.match(sourceLibrary, /<MarkdownView/);
  assert.match(sourceLibrary, /<CodeViewer/);
  assert.match(layout, /\/og\.png/);
  assert.match(css, /white-space:\s*pre/);
  assert.match(css, /\.question-dialog/);
  assert.match(css, /\.knowledge-card/);
  assert.match(css, /\.repository-grid/);
  assert.match(css, /\.repository-workbench/);
  assert.match(css, /\.source-file-tree/);
  assert.doesNotMatch(packageJson, /react-loading-skeleton/);
  await access(new URL("../public/og.png", import.meta.url));
  await assert.rejects(access(new URL("../app/_sites-preview/SkeletonPreview.tsx", import.meta.url)));
  await access(new URL("../scripts/generate-content.mjs", import.meta.url));
  await access(templateRoot);
});

test("keeps all four lazy repository indexes exact and searchable", async () => {
  const expected = {
    "lld-atlas": { branch: null, commit: null, license: "Local canonical material", localPath: "." },
    "awesome-low-level-design": { branch: "main", commit: "fc26e4033cad6d24f32caa8521044febbf065beb", license: "GPL-3.0", localPath: "References/awesome-low-level-design" },
    "low-level-design-primer": { branch: "master", commit: "49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd", license: "No license declared", localPath: "References/low-level-design-primer" },
    "kumaransg-lld": { branch: "main", commit: "1698cc6f993a5014d4370b5e0db9f64d322e2400", license: "No repository-wide license declared", localPath: "References/kumaransg-LLD" },
  };

  for (const [id, metadata] of Object.entries(expected)) {
    const payload = JSON.parse(await readFile(new URL(`../public/repository-content/${id}.json`, import.meta.url), "utf8"));
    assert.equal(payload.id, id);
    assert.equal(payload.branch, metadata.branch);
    assert.equal(payload.commit, metadata.commit);
    assert.equal(payload.license, metadata.license);
    assert.equal(payload.localPath, metadata.localPath);
    assert.equal(payload.entries.length, payload.fileCount);
    assert.ok(payload.entries.every((entry) => typeof entry.path === "string" && typeof entry.generated === "boolean"));
    assert.ok(payload.entries.some((entry) => entry.content !== null));
    if (payload.remote) assert.ok(payload.entries.some((entry) => /^https:\/\/github\.com\//.test(entry.sourceUrl ?? "")));
    if (id === "lld-atlas") assert.ok(payload.entries.some((entry) => entry.path === "References/PRIMER_QUESTION_INDEX.md"));
  }

  const primerIndex = await readFile(new URL("../../References/PRIMER_QUESTION_INDEX.md", import.meta.url), "utf8");
  assert.equal((primerIndex.match(/^\| \d{3} \|/gm) ?? []).length, 140);
  assert.equal((primerIndex.match(/^\| \d{2} \|/gm) ?? []).length, 28);
  assert.equal((primerIndex.match(/\[Solution \d+\]\(/g) ?? []).length, 40);
  assert.equal((primerIndex.match(/\[Video \d+\]\(/g) ?? []).length, 24);
});
