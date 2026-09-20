# Course Registration System

## Problem

Design a university registration domain where students find course offerings and enroll without violating capacity or academic rules.

## Required behavior

- Manage courses, term-specific sections, instructors, rooms, schedules, and enrollment capacity.
- Search the catalog and view seats, prerequisites, and meeting times.
- Enroll or drop a student and expose the student's current schedule.
- Reject prerequisite, timetable, duplicate-enrollment, credit-limit, and registration-window violations.
- Place eligible students on an ordered waitlist and promote them when a seat opens.
- Handle concurrent attempts for the final seat consistently.

## Core model

`Course`, `Section`, `Term`, `Student`, `Instructor`, `Meeting`, `Enrollment`, `WaitlistEntry`, and `RegistrationPolicy`.

Separate the stable catalog course from a section offered in one term. Policies should return explainable violations so clients do not need to reproduce domain rules.

## Invariants and edge cases

- Confirmed enrollment never exceeds section capacity.
- A student cannot be both enrolled and waitlisted for the same section.
- Promotion must re-check prerequisites, conflicts, and credit limits.
- Commands should be idempotent and preserve an enrollment audit trail.

## Design exercise

Demonstrate enrollment, a full-section race, waitlist promotion, a schedule conflict, a failed prerequisite, and dropping after the deadline.
