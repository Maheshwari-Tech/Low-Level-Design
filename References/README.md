# Reference snapshots

This directory contains three clean, full Git clones kept as immutable source snapshots. Curated questions, explanations, and indexes live outside the nested clones so upstream history, attribution, and file identity remain inspectable.

## Verified snapshots

| Local snapshot | Exact workspace path | Remote | Branch | Commit | Commit date | Files | License status |
| --- | --- | --- | --- | --- | --- | ---: | --- |
| [awesome-low-level-design](./awesome-low-level-design/) | `/Users/snju/Desktop/Code/personal/Low level Design/References/awesome-low-level-design` | [ashishps1/awesome-low-level-design](https://github.com/ashishps1/awesome-low-level-design) | `main` | [`fc26e4033cad6d24f32caa8521044febbf065beb`](https://github.com/ashishps1/awesome-low-level-design/tree/fc26e4033cad6d24f32caa8521044febbf065beb) | 2026-02-26 10:45:01 +05:30 | 3,343 | Root [GPL-3.0 license](./awesome-low-level-design/LICENSE) |
| [low-level-design-primer](./low-level-design-primer/) | `/Users/snju/Desktop/Code/personal/Low level Design/References/low-level-design-primer` | [prasadgujar/low-level-design-primer](https://github.com/prasadgujar/low-level-design-primer) | `master` | [`49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd`](https://github.com/prasadgujar/low-level-design-primer/tree/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd) | 2023-05-16 15:44:34 +05:30 | 7 | No repository-level license found |
| [kumaransg-LLD](./kumaransg-LLD/) | `/Users/snju/Desktop/Code/personal/Low level Design/References/kumaransg-LLD` | [kumaransg/LLD](https://github.com/kumaransg/LLD) | `main` | [`1698cc6f993a5014d4370b5e0db9f64d322e2400`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400) | 2024-01-08 20:02:07 +05:30 | 4,171 | No repository-level license; four embedded projects carry their own licenses |

The file count is both the number returned by `git ls-files` and the number of non-`.git` files in each snapshot. At verification time, all three nested repositories had an empty `git status --short`, their checked-out branches matched `origin/HEAD`, and `git rev-parse --is-shallow-repository` returned `false`.

## License boundaries

The `awesome-low-level-design` snapshot is covered by its root GPL-3.0 license.

Neither `low-level-design-primer` nor `kumaransg-LLD` declares a repository-wide license at the pinned commit. Absence of a license is not permission to redistribute or adapt the contents. Their material therefore remains an unmodified reference snapshot; original notes in this workspace should summarize ideas and link to provenance rather than copy unlicensed text or code.

Four `kumaransg-LLD` subprojects have explicit local licenses:

- [ride-sharing-low-level-design](<./kumaransg-LLD/Ride Sharing /ride-sharing-low-level-design/LICENSE>) — MIT, copyright Amar Prakash Pandey.
- [RideShare_MachineCoding_Sample](<./kumaransg-LLD/Ride Sharing /RideShare_MachineCoding_Sample/LICENSE>) — MIT, copyright Akshansh Ohm.
- [cache-low-level-design](./kumaransg-LLD/Low_level_Design_Problems/cache-low-level-design/LICENSE) — MIT, copyright Amar Prakash Pandey.
- [parkinglot](./kumaransg-LLD/Low_level_Design_Problems/parkinglot/LICENSE) — Apache License 2.0.

Each license applies to its own subtree, not automatically to the aggregate repository.

## Immutability rule

The directories `References/awesome-low-level-design`, `References/low-level-design-primer`, and `References/kumaransg-LLD` retain their own `.git` directories and stay unmodified. Do not format, deduplicate, delete build output, or fix code inside them. That preservation serves three purposes:

1. `git status --short` remains a direct integrity check.
2. Every note can point to an exact path and pinned upstream commit.
3. Upstream duplicates and incomplete examples remain evidence, while the canonical problem bank can present a clean structure separately.

Add workspace-owned explanations beside this file or under `Problems/`, never inside a nested clone. The [Kumar variation index](./VARIATION_INDEX.md) reconciles the largest aggregate, while the [primer coverage index](./PRIMER_QUESTION_INDEX.md) accounts for all 140 prompts, 28 solution topics, 40 solution links, and 24 video links.

## Reproduce the verification

Run these commands from the workspace root, once for each nested repository:

```bash
git -C References/awesome-low-level-design remote get-url origin
git -C References/awesome-low-level-design branch --show-current
git -C References/awesome-low-level-design rev-parse HEAD
git -C References/awesome-low-level-design rev-parse --is-shallow-repository
git -C References/awesome-low-level-design status --short
git -C References/awesome-low-level-design ls-files | wc -l
```

Repeat with `References/low-level-design-primer` and `References/kumaransg-LLD`. A blank status is expected; any output means the reference snapshot was changed.
