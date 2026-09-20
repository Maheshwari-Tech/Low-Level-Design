# Low Level Design Screenshot Verification

Audited on 2026-08-09 using the signed-in desktop web view.

## Capture rules

- Desktop viewport: 1600 × 1200 CSS pixels.
- Every lesson is captured from the top through the bottom with approximately 15% overlap.
- Every screenshot is the full 1200-pixel viewport height, exceeding the required 50% minimum.
- Solution accordions are expanded before the main reading sequence is captured.
- Every alternate code-file tab is captured separately in the selected Python language.
- Wide lesson code or table regions receive horizontal supplemental captures.
- Community-comment overflow and the separate voting page are excluded from lesson-content supplements.

## Overall result

| Pages | Verified | Reading frames | Code-tab supplements | Wide supplements | Solutions expanded | Total JPEGs |
|---:|---:|---:|---:|---:|---:|---:|
| 18 | 18 | 503 | 36 | 76 | 52 | 615 |

## Section totals

| Section | Pages | Reading frames | Code tabs | Wide | Expanded | Total JPEGs |
|---|---:|---:|---:|---:|---:|---:|
| In a Hurry | 5 | 70 | 3 | 4 | 0 | 77 |
| Concurrency | 4 | 76 | 0 | 0 | 0 | 76 |
| Problem Breakdowns | 9 | 357 | 33 | 72 | 52 | 462 |

## Page verification

| Section | # | Page | Reading | Code tabs | Wide | Expanded | Status |
|---|---:|---|---:|---:|---:|---:|---|
| In a Hurry | 1 | Introduction | 9 | 3 | 2 | 0 | Verified |
| In a Hurry | 2 | Delivery Framework | 14 | 0 | 2 | 0 | Verified |
| In a Hurry | 3 | Design Principles | 18 | 0 | 0 | 0 | Verified |
| In a Hurry | 4 | OOP Concepts | 13 | 0 | 0 | 0 | Verified |
| In a Hurry | 5 | Design Patterns | 16 | 0 | 0 | 0 | Verified |
| Concurrency | 1 | Introduction | 10 | 0 | 0 | 0 | Verified |
| Concurrency | 2 | Correctness | 25 | 0 | 0 | 0 | Verified |
| Concurrency | 3 | Coordination | 21 | 0 | 0 | 0 | Verified |
| Concurrency | 4 | Scarcity | 20 | 0 | 0 | 0 | Verified |
| Problem Breakdowns | 1 | Connect Four | 34 | 2 | 8 | 4 | Verified |
| Problem Breakdowns | 2 | Amazon Locker | 32 | 3 | 0 | 3 | Verified |
| Problem Breakdowns | 3 | Elevator | 41 | 2 | 22 | 8 | Verified |
| Problem Breakdowns | 4 | Parking Lot | 37 | 2 | 6 | 8 | Verified |
| Problem Breakdowns | 5 | File System | 42 | 3 | 2 | 8 | Verified |
| Problem Breakdowns | 6 | Movie Ticket Booking | 43 | 4 | 10 | 3 | Verified |
| Problem Breakdowns | 7 | Logging Service | 38 | 9 | 2 | 8 | Verified |
| Problem Breakdowns | 8 | Rate Limiter | 42 | 5 | 14 | 2 | Verified |
| Problem Breakdowns | 9 | Inventory Management | 48 | 3 | 8 | 8 | Verified |

## Final filesystem checks

- All 18 expected lesson folders are present.
- Every reading sequence begins at `001` and contains no numbering gaps.
- All 615 JPEGs decode successfully at exactly 1600 × 1200 pixels.
- Recorded reading, tab, and wide-capture counts match the files on disk.
- No lesson has remaining collapsed solution controls, missing top or bottom coverage, or unresolved wide lesson content.
