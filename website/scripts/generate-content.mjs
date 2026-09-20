import { execFile } from "node:child_process";
import { mkdir, readFile, readdir, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const websiteRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const repositoryRoot = path.resolve(websiteRoot, "..");
const problemsRoot = path.join(repositoryRoot, "Problems");
const patternsRoot = path.join(repositoryRoot, "Patterns");
const principlesRoot = path.join(repositoryRoot, "Principles");
const execFileAsync = promisify(execFile);

const repositoryDefinitions = [
  {
    id: "awesome-low-level-design",
    title: "awesome-low-level-design",
    owner: "ashishps1",
    repository: "awesome-low-level-design",
    root: path.join(repositoryRoot, "References", "awesome-low-level-design"),
    localPath: "References/awesome-low-level-design",
    license: "GPL-3.0",
    role: "Multi-language LLD solutions, OOP examples, diagrams, and design-pattern implementations.",
  },
  {
    id: "low-level-design-primer",
    title: "low-level-design-primer",
    owner: "prasadgujar",
    repository: "low-level-design-primer",
    root: path.join(repositoryRoot, "References", "low-level-design-primer"),
    localPath: "References/low-level-design-primer",
    license: "No license declared",
    role: "Question prompts, external solution references, books, blogs, and learning resources.",
  },
  {
    id: "kumaransg-lld",
    title: "kumaransg/LLD",
    owner: "kumaransg",
    repository: "LLD",
    root: path.join(repositoryRoot, "References", "kumaransg-LLD"),
    localPath: "References/kumaransg-LLD",
    license: "No repository-wide license declared",
    role: "A large machine-coding archive with multiple Java variations for the same interview problem.",
  },
];

const textExtensions = new Set([
  ".bat", ".c", ".cc", ".cfg", ".cmd", ".cpp", ".cql", ".cs", ".csproj",
  ".css", ".editorconfig", ".go", ".gradle", ".h", ".hpp", ".html", ".java",
  ".js", ".json", ".jsx", ".md", ".mod", ".properties", ".props", ".puml",
  ".py", ".rs", ".sh", ".sln", ".sql", ".targets", ".toml", ".ts", ".tsx",
  ".txt", ".xml", ".yaml", ".yml",
]);

const sourceExtensions = new Set([
  ".c", ".cc", ".cpp", ".cs", ".go", ".h", ".hpp", ".java", ".js", ".jsx",
  ".py", ".rs", ".ts", ".tsx",
]);

const generatedPattern = /(^|\/)(?:\.git|\.idea|\.gradle|\.settings|\.vs|bin|build|classes|node_modules|obj|out|target)(\/|$)|(?:\.class|\.dll|\.dylib|\.exe|\.iml|\.jar|\.kotlin_module|\.o|\.pdb|\.pyc|\.so|\.suo|\.zip)$|(?:^|\/)\.DS_Store$/i;

const lessonDefinitions = [
  {
    slug: "senior_staff_lld_interview",
    title: "60-Minute Senior/Staff LLD Interview",
    group: "Interview preparation",
    summary: "A timed answer method with clarifying questions, invariants, APIs, SOLID and pattern reasoning, one-hour Python scope, concurrency, follow-ups, and scoring.",
    document: "Foundations/senior_staff_lld_interview.md",
  },
  {
    slug: "object_oriented_programming",
    title: "Object-Oriented Programming",
    group: "Foundations",
    summary: "Objects, encapsulation, abstraction, inheritance, polymorphism, and composition in interview designs.",
    document: "Foundations/object_oriented_programming.md",
  },
  {
    slug: "class_relationships",
    title: "Class Relationships",
    group: "Foundations",
    summary: "Association, aggregation, composition, dependency, inheritance, and how to choose between them.",
    document: "Foundations/class_relationships.md",
  },
  {
    slug: "clean_code",
    title: "Clean Code for LLD",
    group: "Foundations",
    summary: "Names, boundaries, cohesion, error handling, and keeping an interview solution readable under change.",
    document: "Foundations/clean_code.md",
  },
  {
    slug: "testing_lld",
    title: "Testing Low-Level Designs",
    group: "Foundations",
    summary: "Test invariants, state transitions, failure paths, time, concurrency, and external ports deterministically.",
    document: "Foundations/testing.md",
  },
  {
    slug: "concurrency_fundamentals",
    title: "Concurrency Fundamentals",
    group: "Concurrency",
    summary: "Memory visibility, atomicity, coordination primitives, lock design, and runnable Java exercises.",
    document: "Concurrency/README.md",
    sourceRoot: "Concurrency/com",
  },
  {
    slug: "java_concurrency_lld_interviews",
    title: "Java Multithreading & Concurrency for LLD Interviews",
    group: "Concurrency",
    summary: "ExecutorService, bounded BlockingQueue backpressure, semaphores, synchronization, failure handling, deterministic tests, and Senior/Staff trade-offs.",
    document: "Concurrency/JAVA_MULTITHREADING_INTERVIEW_GUIDE.md",
    sourceRoot: "Concurrency/interview",
  },
  {
    slug: "multithreading_introduction",
    title: "Multithreading Introduction",
    group: "Concurrency",
    summary: "Threads, scheduling, shared state, race conditions, and the vocabulary needed for LLD interviews.",
    document: "Concurrency/Multithreading/Introduction.md",
  },
  {
    slug: "concurrency_questions",
    title: "Concurrency Question Bank",
    group: "Concurrency",
    summary: "Practice prompts for producer-consumer, coordination, safety, liveness, and bounded-resource designs.",
    document: "Concurrency/questions/README.md",
  },
  {
    slug: "uml_for_lld",
    title: "UML for Low-Level Design",
    group: "Modeling",
    summary: "Choose class, sequence, activity, state, and use-case diagrams based on the design question being answered.",
    document: "UML/README.md",
  },
];

const featuredDefinitions = [
  {
    slug: "order_processing_system",
    title: "Order Management System",
    eyebrow: "Commerce orchestration",
    summary: "A commercial order aggregate with immutable price snapshots, explicit transitions, idempotent commands, and compensated inventory/payment coordination.",
    focus: ["State machine", "Saga compensation", "Idempotency", "Optimistic concurrency"],
    invariants: [
      "An order is confirmed only after inventory and payment both succeed.",
      "Fulfilled quantity never exceeds confirmed, non-cancelled quantity.",
      "A retried command returns its original result without repeating side effects.",
    ],
    accent: "cobalt",
  },
  {
    slug: "inventory_reservation_service",
    title: "Inventory Reservation Service",
    eyebrow: "Scarce-resource safety",
    summary: "Atomic multi-SKU holds with confirm, release, expiry, deterministic allocation, and one-winner concurrency at the final unit.",
    focus: ["Atomic allocation", "TTL expiry", "Lock ordering", "Injected clock"],
    invariants: [
      "Available stock is always on-hand minus reserved and never goes negative.",
      "A multi-line reservation commits every line or none of them.",
      "Confirm, release, and expiry consume a hold exactly once.",
    ],
    accent: "mint",
  },
  {
    slug: "coupon_promotion_engine",
    title: "Coupon / Promotion Engine",
    eyebrow: "Composable pricing policy",
    summary: "Typed conditions and benefits for coupons, automatic campaigns, stacking, exclusivity, caps, preview, and atomic redemption limits.",
    focus: ["Policy composition", "Exact money", "Deterministic stacking", "Usage limits"],
    invariants: [
      "Preview never consumes a redemption.",
      "Discounts cannot drive a line or order total below zero.",
      "Global and customer redemption counters move atomically.",
    ],
    accent: "amber",
  },
  {
    slug: "notification_framework",
    title: "Notification Framework",
    eyebrow: "Reliable multichannel delivery",
    summary: "Versioned templates, locale fallback, recipient preferences, provider adapters, retries, deduplication, and an auditable delivery history.",
    focus: ["Adapter ports", "Retry policy", "Dedupe", "Partial delivery"],
    invariants: [
      "Consent and quiet-hour rules are checked before provider delivery.",
      "Fallback attempts belong to one logical notification.",
      "Duplicate or stale callbacks cannot regress terminal state.",
    ],
    accent: "violet",
  },
  {
    slug: "payment_processing_service",
    title: "Payment Processing Service",
    eyebrow: "Money and unknown outcomes",
    summary: "Provider-neutral authorize, capture, void, and refund flows with partial amounts, callback reconciliation, and side-effect-safe retries.",
    focus: ["Exact money", "Unknown outcomes", "Reconciliation", "Provider ports"],
    invariants: [
      "Captured total cannot exceed the authorization.",
      "Refunded total cannot exceed the captured total.",
      "A timeout remains unknown until the provider result is reconciled.",
    ],
    accent: "rose",
  },
  {
    slug: "warehouse_fulfilment_domain",
    title: "Warehouse Fulfilment Domain",
    eyebrow: "Physical execution",
    summary: "Split allocation, atomically claimed pick tasks, validated scans, short-pick recovery, packaging, shipping, and upstream status events.",
    focus: ["Task claiming", "Scan idempotency", "Split fulfilment", "Exception recovery"],
    invariants: [
      "Picked, packed, and shipped quantities never exceed allocation.",
      "Only one worker can claim a pick task.",
      "A package contains only picked, previously unpacked quantities.",
    ],
    accent: "cyan",
  },
  {
    slug: "rate_limiter",
    title: "Rate Limiter",
    eyebrow: "Concurrent quota control",
    summary: "Pluggable fixed-window and exact token-bucket policies with per-key isolation, weighted costs, live rules, and idle-state eviction.",
    focus: ["Token bucket", "Fixed window", "Per-key locks", "Boundary semantics"],
    invariants: [
      "Concurrent callers cannot spend the final unit more than once.",
      "Rejected requests do not consume capacity.",
      "Unrelated keys do not serialize one another.",
    ],
    accent: "lime",
  },
  {
    slug: "product_catalog_service",
    title: "Product Catalog Service",
    eyebrow: "Typed product truth",
    summary: "Category schemas, validated attributes and variants, stable IDs and SKUs, optimistic versions, lifecycle control, and schema-impact reports.",
    focus: ["Typed attributes", "SKU uniqueness", "Version history", "Schema evolution"],
    invariants: [
      "A product activates only when every required attribute is valid.",
      "Stable SKUs cannot be reused for another product.",
      "One of two stale concurrent updates fails with a version conflict.",
    ],
    accent: "orange",
  },
];

async function walkJavaFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await walkJavaFiles(absolute)));
    } else if (entry.isFile() && entry.name.endsWith(".java")) {
      files.push(absolute);
    }
  }

  return files;
}

async function walkPythonFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory() && entry.name !== "__pycache__") {
      files.push(...(await walkPythonFiles(absolute)));
    } else if (entry.isFile() && entry.name.endsWith(".py")) {
      files.push(absolute);
    }
  }

  return files;
}

function chooseEntryFile(files, slug) {
  const preferredNames = {
    order_processing_system: "OrderService.java",
    inventory_reservation_service: "InventoryReservationService.java",
    coupon_promotion_engine: "PromotionEngine.java",
    notification_framework: "NotificationService.java",
    payment_processing_service: "PaymentService.java",
    warehouse_fulfilment_domain: "WarehouseFulfilmentService.java",
    rate_limiter: "RateLimiter.java",
    product_catalog_service: "ProductCatalogService.java",
  };
  const preferred = files.find((file) => file.path.endsWith(preferredNames[slug]));
  const architecturalEntry = files.find((file) =>
    /(?:Service|System|Manager|Controller|Engine|Limiter)\.java$/.test(file.path),
  );
  return preferred?.path ??
    architecturalEntry?.path ??
    files.find((file) => /(?:Demo|Main)\.java$/.test(file.path))?.path ??
    files[0]?.path ??
    "";
}

function choosePythonEntryFile(files) {
  return files.find((file) => file.path === "python/solution.py")?.path ??
    files.find((file) => file.path.endsWith("/solution.py"))?.path ??
    files.find((file) => !file.path.endsWith("test_solution.py"))?.path ??
    files[0]?.path ??
    "";
}

function parseCatalog(indexMarkdown) {
  const entries = [];
  const seen = new Set();
  let category = "Other systems";

  for (const line of indexMarkdown.split("\n")) {
    const categoryMatch = /^###\s+(.+)$/.exec(line);
    if (categoryMatch) {
      category = categoryMatch[1];
      continue;
    }

    const match = /^- \[([^\]]+)\]\(([^)]+)\) — (.+)$/.exec(line);
    if (!match) continue;
    const [, title, href, summary] = match;
    const slug = href.replace(/\/$/, "");
    if (slug.includes("/") || seen.has(slug)) continue;
    seen.add(slug);
    entries.push({ title, slug, summary, category });
  }
  return entries;
}

function stripInlineMarkdown(value) {
  return value
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/`([^`]+)`/g, "$1")
    .trim();
}

function slugFromPath(value) {
  return value
    .replace(/README\.md$/i, "")
    .replace(/\.md$/i, "")
    .replace(/\/$/, "")
    .replace(/[^a-zA-Z0-9]+/g, "_")
    .replace(/^_|_$/g, "")
    .toLowerCase();
}

function parsePatternIndex(indexMarkdown) {
  const entries = [];
  let group = "Design pattern";

  for (const line of indexMarkdown.split("\n")) {
    const groupMatch = /^##\s+(Creational|Structural|Behavioral) patterns$/i.exec(line);
    if (groupMatch) {
      group = `${groupMatch[1][0].toUpperCase()}${groupMatch[1].slice(1).toLowerCase()}`;
      continue;
    }

    const match = /^\|\s*\[([^\]]+)\]\(([^)]+)\)\s*\|\s*(.*?)\s*\|$/.exec(line);
    if (!match) continue;
    const [, title, href, summary] = match;
    entries.push({ title, href, summary: stripInlineMarkdown(summary), group, slug: slugFromPath(href) });
  }

  return entries;
}

function parsePrincipleIndex(indexMarkdown) {
  const entries = [];

  for (const line of indexMarkdown.split("\n")) {
    if (!line.startsWith("|") || /^\|\s*-/.test(line)) continue;
    const cells = line.slice(1, -1).split("|").map((cell) => cell.trim());
    if (cells.length !== 3) continue;
    const link = /\[([^\]]+)\]\(([^)]+)\)/.exec(cells[2]);
    if (!link) continue;
    entries.push({
      title: link[1],
      href: link[2],
      summary: stripInlineMarkdown(cells[1]),
      group: "SOLID",
      slug: slugFromPath(link[2]),
    });
  }

  entries.push({
    title: "DRY, KISS, YAGNI & Law of Demeter",
    href: "other.md",
    summary: "Complementary heuristics for duplication, simplicity, scope control, and object collaboration.",
    group: "Complementary",
    slug: "complementary_principles",
  });
  return entries;
}

async function buildKnowledgeDetails(root, definitions) {
  return Promise.all(definitions.map(async (definition) => {
    const documentPath = definition.href.endsWith("/")
      ? path.join(root, definition.href, "README.md")
      : path.join(root, definition.href);
    const sourceRoot = definition.href.endsWith("/")
      ? path.join(root, definition.href)
      : path.dirname(documentPath);
    let javaPaths = definition.href.endsWith("/") ? await walkJavaFiles(sourceRoot) : [];
    if (root === patternsRoot && definition.slug === "structural_decorator") {
      javaPaths = javaPaths.filter((absolute) =>
        !path.relative(sourceRoot, absolute).split(path.sep).join("/").startsWith("good/"),
      );
    }
    const files = await Promise.all(javaPaths.map(async (absolute) => ({
      path: path.relative(sourceRoot, absolute).split(path.sep).join("/"),
      code: await readFile(absolute, "utf8"),
    })));
    return {
      title: definition.title,
      slug: definition.slug,
      summary: definition.summary,
      group: definition.group,
      javaFileCount: files.length,
      entryFile: chooseEntryFile(files, definition.slug),
      readme: await readFile(documentPath, "utf8"),
      files,
    };
  }));
}

async function buildLessonDetails() {
  return Promise.all(lessonDefinitions.map(async (definition) => {
    const sourceRoot = definition.sourceRoot
      ? path.join(repositoryRoot, definition.sourceRoot)
      : null;
    const javaPaths = sourceRoot ? await walkJavaFiles(sourceRoot) : [];
    const files = await Promise.all(javaPaths.map(async (absolute) => ({
      path: path.relative(sourceRoot, absolute).split(path.sep).join("/"),
      code: await readFile(absolute, "utf8"),
    })));
    return {
      title: definition.title,
      slug: definition.slug,
      summary: definition.summary,
      group: definition.group,
      javaFileCount: files.length,
      entryFile: chooseEntryFile(files, definition.slug),
      readme: await readFile(path.join(repositoryRoot, definition.document), "utf8"),
      files,
    };
  }));
}

function knowledgeIndex(details) {
  return details.map((item) => ({
    title: item.title,
    slug: item.slug,
    summary: item.summary,
    group: item.group,
    javaFileCount: item.javaFileCount,
    entryFile: item.entryFile,
  }));
}

function repositoryIndex(payload) {
  return {
    id: payload.id,
    title: payload.title,
    commit: payload.commit,
    branch: payload.branch,
    remote: payload.remote,
    localPath: payload.localPath,
    license: payload.license,
    role: payload.role,
    fileCount: payload.fileCount,
    previewableFiles: payload.previewableFiles,
    bytes: payload.bytes,
    byKind: payload.byKind,
    byLanguage: payload.byLanguage,
  };
}

function languageFor(filePath) {
  const extension = path.extname(filePath).toLowerCase();
  const names = {
    ".c": "C",
    ".cc": "C++",
    ".cpp": "C++",
    ".cs": "C#",
    ".cql": "CQL",
    ".css": "CSS",
    ".go": "Go",
    ".gradle": "Gradle",
    ".h": "C/C++",
    ".hpp": "C++",
    ".html": "HTML",
    ".java": "Java",
    ".js": "JavaScript",
    ".json": "JSON",
    ".jsx": "JavaScript",
    ".md": "Markdown",
    ".pdf": "PDF",
    ".properties": "Properties",
    ".puml": "PlantUML",
    ".py": "Python",
    ".rs": "Rust",
    ".sql": "SQL",
    ".ts": "TypeScript",
    ".tsx": "TypeScript",
    ".xml": "XML",
    ".yaml": "YAML",
    ".yml": "YAML",
  };
  return names[extension] ?? (extension ? extension.slice(1).toUpperCase() : "File");
}

function kindFor(filePath) {
  if (generatedPattern.test(filePath)) return "Generated / binary";
  const extension = path.extname(filePath).toLowerCase();
  if (sourceExtensions.has(extension)) return "Source code";
  if ([".md", ".pdf", ".txt"].includes(extension)) return "Notes / document";
  if ([".jpeg", ".jpg", ".png", ".puml", ".svg"].includes(extension)) return "Diagram / media";
  if ([".csproj", ".gradle", ".json", ".mod", ".properties", ".props", ".sln", ".targets", ".toml", ".xml", ".yaml", ".yml"].includes(extension)) return "Build / configuration";
  return "Other tracked file";
}

function githubBlobUrl(definition, commit, filePath) {
  const encodedPath = filePath.split("/").map(encodeURIComponent).join("/");
  return `https://github.com/${definition.owner}/${definition.repository}/blob/${commit}/${encodedPath}`;
}

async function gitValue(root, args) {
  const { stdout } = await execFileAsync("git", args, {
    cwd: root,
    encoding: "utf8",
    maxBuffer: 10 * 1024 * 1024,
  });
  return stdout.trim();
}

async function trackedPaths(root) {
  const output = await gitValue(root, ["ls-files", "-z"]);
  return output.split("\0").filter(Boolean).sort((left, right) => left.localeCompare(right));
}

async function workspaceLearningPaths() {
  const roots = [
    "CHANGELOG.md",
    "COVERAGE.md",
    "README.md",
    "RESOURCES.md",
    "Foundations",
    "Principles",
    "Patterns",
    "Concurrency",
    "UML",
    "Problems",
    "References/README.md",
    "References/VARIATION_INDEX.md",
    "References/PRIMER_QUESTION_INDEX.md",
    "website/README.md",
  ];
  const files = [];

  async function visit(relativePath) {
    const absolute = path.join(repositoryRoot, relativePath);
    const info = await stat(absolute);
    if (info.isFile()) {
      if (!generatedPattern.test(relativePath)) files.push(relativePath);
      return;
    }
    const entries = await readdir(absolute, { withFileTypes: true });
    for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
      const child = path.join(relativePath, entry.name);
      if (entry.isDirectory() && generatedPattern.test(`${child}/`)) continue;
      if (entry.isDirectory() || entry.isFile()) await visit(child);
    }
  }

  for (const root of roots) {
    try {
      await visit(root);
    } catch (error) {
      if (error?.code !== "ENOENT") throw error;
    }
  }
  return files.sort((left, right) => left.localeCompare(right));
}

async function libraryEntry(root, relativePath, sourceUrl) {
  const absolute = path.join(root, relativePath);
  const info = await stat(absolute);
  const extension = path.extname(relativePath).toLowerCase();
  const generated = generatedPattern.test(relativePath);
  const previewable = textExtensions.has(extension) && info.size <= 1024 * 1024;
  return {
    path: relativePath.split(path.sep).join("/"),
    topLevel: relativePath.split(path.sep)[0],
    kind: kindFor(relativePath),
    language: languageFor(relativePath),
    size: info.size,
    sourceUrl,
    generated,
    content: previewable ? await readFile(absolute, "utf8") : null,
  };
}

function repositorySummary(entries) {
  const byKind = {};
  const byLanguage = {};
  let bytes = 0;
  let previewableFiles = 0;
  for (const entry of entries) {
    byKind[entry.kind] = (byKind[entry.kind] ?? 0) + 1;
    byLanguage[entry.language] = (byLanguage[entry.language] ?? 0) + 1;
    bytes += entry.size;
    if (entry.content !== null) previewableFiles += 1;
  }
  return { fileCount: entries.length, previewableFiles, bytes, byKind, byLanguage };
}

async function buildRepositoryLibraries() {
  const outputRoot = path.join(websiteRoot, "public", "repository-content");
  await mkdir(outputRoot, { recursive: true });
  const indexes = [];

  const localPaths = await workspaceLearningPaths();
  const localEntries = [];
  for (const relativePath of localPaths) {
    localEntries.push(await libraryEntry(repositoryRoot, relativePath, null));
  }
  const localPayload = {
    id: "lld-atlas",
    title: "LLD Atlas canonical repository",
    commit: null,
    branch: null,
    remote: null,
    localPath: ".",
    license: "Local canonical material",
    role: "Maintained learning notes, interview questions, patterns, principles, UML, concurrency, and Java/Python reference solutions.",
    ...repositorySummary(localEntries),
    entries: localEntries,
  };
  await writeFile(path.join(outputRoot, "lld-atlas.json"), `${JSON.stringify(localPayload)}\n`, "utf8");
  indexes.push(repositoryIndex(localPayload));

  for (const definition of repositoryDefinitions) {
    const [commit, branch, remote, paths] = await Promise.all([
      gitValue(definition.root, ["rev-parse", "HEAD"]),
      gitValue(definition.root, ["branch", "--show-current"]),
      gitValue(definition.root, ["remote", "get-url", "origin"]),
      trackedPaths(definition.root),
    ]);
    const entries = [];
    for (const relativePath of paths) {
      entries.push(await libraryEntry(
        definition.root,
        relativePath,
        githubBlobUrl(definition, commit, relativePath),
      ));
    }
    const payload = {
      id: definition.id,
      title: definition.title,
      commit,
      branch,
      remote,
      localPath: definition.localPath,
      license: definition.license,
      role: definition.role,
      ...repositorySummary(entries),
      entries,
    };
    await writeFile(path.join(outputRoot, `${definition.id}.json`), `${JSON.stringify(payload)}\n`, "utf8");
    indexes.push(repositoryIndex(payload));
  }
  return indexes;
}

const problemIndex = await readFile(path.join(problemsRoot, "README.md"), "utf8");
const patternIndex = await readFile(path.join(patternsRoot, "README.md"), "utf8");
const principleIndex = await readFile(path.join(principlesRoot, "README.md"), "utf8");
const catalogDetails = await Promise.all(
  parseCatalog(problemIndex).map(async (problem) => {
    const root = path.join(problemsRoot, problem.slug);
    const [javaPaths, pythonPaths] = await Promise.all([
      walkJavaFiles(root),
      walkPythonFiles(root),
    ]);
    const files = await Promise.all(
      [...javaPaths, ...pythonPaths].map(async (absolute) => ({
        path: path.relative(root, absolute).split(path.sep).join("/"),
        code: await readFile(absolute, "utf8"),
      })),
    );
    files.sort((left, right) => left.path.localeCompare(right.path));
    const readme = await readFile(path.join(root, "README.md"), "utf8");
    return {
      ...problem,
      javaFileCount: javaPaths.length,
      pythonFileCount: pythonPaths.length,
      entryFile: chooseEntryFile(files, problem.slug),
      pythonEntryFile: choosePythonEntryFile(files),
      readme,
      files,
    };
  }),
);
const patternDetails = await buildKnowledgeDetails(patternsRoot, parsePatternIndex(patternIndex));
const principleDetails = await buildKnowledgeDetails(principlesRoot, parsePrincipleIndex(principleIndex));
const lessonDetails = await buildLessonDetails();
const repositories = await buildRepositoryLibraries();

const catalog = catalogDetails.map((item) => ({
  title: item.title,
  slug: item.slug,
  summary: item.summary,
  category: item.category,
  javaFileCount: item.javaFileCount,
  pythonFileCount: item.pythonFileCount,
  entryFile: item.entryFile,
  pythonEntryFile: item.pythonEntryFile,
}));

const catalogBySlug = new Map(catalogDetails.map((problem) => [problem.slug, problem]));
const featured = featuredDefinitions.map((definition) => {
  const detail = catalogBySlug.get(definition.slug);
  if (!detail) throw new Error(`Featured problem is missing from the catalog: ${definition.slug}`);
  const variationCount = (detail.readme.match(/^#### Variation \d+\b/gm) ?? []).length;
  if (variationCount === 0) {
    throw new Error(`Featured problem has no merged interview variations: ${definition.slug}`);
  }
  if (!detail.readme.includes("### One-hour Python coding scope")) {
    throw new Error(`Featured problem has no one-hour Python scope: ${definition.slug}`);
  }
  if (detail.pythonFileCount < 2) {
    throw new Error(`Featured problem needs solution.py and test_solution.py: ${definition.slug}`);
  }
  return {
    ...definition,
    level: "Senior / Staff",
    durationMinutes: 60,
    variationCount,
    javaFileCount: detail.javaFileCount,
    pythonFileCount: detail.pythonFileCount,
    fileCount: detail.javaFileCount + detail.pythonFileCount,
    entryFile: detail.entryFile,
    pythonEntryFile: detail.pythonEntryFile,
  };
});

const publicPayload = {
  schemaVersion: 4,
  catalog: catalogDetails,
  principles: principleDetails,
  patterns: patternDetails,
  lessons: lessonDetails,
  repositories,
};

const indexPayload = {
  featured,
  catalog,
  principles: knowledgeIndex(principleDetails),
  patterns: knowledgeIndex(patternDetails),
  lessons: knowledgeIndex(lessonDetails),
  repositories,
};

await writeFile(
  path.join(websiteRoot, "public", "lld-content.json"),
  `${JSON.stringify(publicPayload)}\n`,
  "utf8",
);
await writeFile(
  path.join(websiteRoot, "app", "content.generated.ts"),
  `// Generated by scripts/generate-content.mjs. Do not edit by hand.\nexport const contentIndex = ${JSON.stringify(indexPayload, null, 2)} as const;\n`,
  "utf8",
);

console.log(
  `Generated ${catalog.length} questions, ${lessonDetails.length} learning guides, ${principleDetails.length} principle guides, ${patternDetails.length} pattern guides, and ${repositories.reduce((sum, repository) => sum + repository.fileCount, 0)} indexed learning-library files.`,
);
