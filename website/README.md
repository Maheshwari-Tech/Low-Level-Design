# LLD Atlas website

LLD Atlas is the learning platform for this low-level design workspace. It provides a 60-minute Senior/Staff interview playbook, a guided Foundations/Concurrency/UML path, a dedicated Java concurrency interview guide and lab, clickable SOLID and GoF catalogs, a deduplicated interview-question bank, and eight featured question-and-solution packs with runnable Java 17 plus one-hour Python core designs.

## What it reads

The site derives its content from the repository instead of maintaining a second hand-written copy:

- `../Problems/README.md` supplies the deduplicated question index.
- Every canonical folder under `../Problems/` supplies its README and its Java/Python source files.
- `../Foundations/senior_staff_lld_interview.md` supplies the general one-hour interview method, follow-up answers, and scoring rubric.
- `../Concurrency/JAVA_MULTITHREADING_INTERVIEW_GUIDE.md` and `../Concurrency/interview` supply the Java multithreading roadmap and its deterministic Java 17 lab.
- `../Foundations`, `../Concurrency`, and `../UML` supply the guided learning modules.
- `../Principles` supplies all five SOLID guides plus complementary heuristics and their Java examples.
- `../Patterns` supplies all 23 GoF guides and runnable Java examples.
- `../References/*` are real nested Git clones. Their tracked files, exact remotes, branches, commits, license boundaries, notes, and code are indexed without modifying the clones.
- `scripts/generate-content.mjs` writes `app/content.generated.ts`, `public/lld-content.json`, and four lazy `public/repository-content/*.json` indexes.

The eight featured packs open on the interview guide first. Together they merge the source material into 47 numbered, deduplicated interview variations, with a Senior solution, Staff-level extension, canonical-code mapping, and inline provenance for every variation. The source workbench then offers an explicit Java/Python switch. Python modules stay focused on the aggregate, policies, ports, concurrency boundary, and deterministic tests that fit a one-hour round; framework and persistence boilerplate remains design discussion. Run the generator after changing a featured solution. Both `dev` and `build` run it automatically.

## Local development

Prerequisites: Node.js 22.13 or newer and pnpm.

```bash
pnpm install
pnpm dev
```

Open `http://localhost:4105`. The development command uses strict port binding and exits if the registered port is unexpectedly occupied.

## Verification

```bash
pnpm content:refresh
pnpm lint
pnpm build
node --test tests/rendered-html.test.mjs
```

The generated explanation-and-code payload is fetched separately from the server-rendered page. Repository indexes are split into four additional lazy payloads, so thousands of files are downloaded only after a learner opens that repository. Generated/IDE/binary artifacts remain indexed for completeness but are hidden by default.

## Structure

```text
website/
├── app/
│   ├── components/             # lesson, knowledge, question, source, README, and code explorers
│   ├── content.generated.ts    # generated lightweight index
│   ├── globals.css             # responsive editorial UI
│   ├── layout.tsx              # metadata and social card
│   └── page.tsx                # homepage composition
├── public/
│   ├── lld-content.json        # generated READMEs and source files
│   ├── repository-content/     # one lazy exact-source index per repository
│   └── og.png                  # social preview image
├── scripts/generate-content.mjs
└── tests/rendered-html.test.mjs
```

## Code-rendering rules

- Source text is never converted through `dangerouslySetInnerHTML`.
- Every line keeps its original whitespace with `white-space: pre` and a four-space tab width.
- Long lines scroll horizontally instead of wrapping or overlapping line numbers.
- Multi-file solutions are grouped by folder, with the selected file kept visible in the tree.
- The compact syntax highlighter labels and colors Java, Python, Go, C/C++, C#, JavaScript/TypeScript, Rust, SQL, and common text/config formats while keeping source escaped.
- README code fences use the same monospace, preformatted treatment.
- Relative links in canonical guides resolve to the matching lesson, principle, pattern, or question. Relative links in cloned READMEs resolve to the exact pinned GitHub revision.

## Upstream attribution and license boundaries

The source library is generated from the exact local clones documented in [`../References/README.md`](../References/README.md). `ashishps1/awesome-low-level-design` is GPL-3.0. `prasadgujar/low-level-design-primer` and the aggregate `kumaransg/LLD` repository declare no repository-wide license at their pinned revisions; the UI states that boundary and links every file back to its exact upstream source. Do not publish or redistribute those unlicensed payloads without permission.
