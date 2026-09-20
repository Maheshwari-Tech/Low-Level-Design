# Online Learning Platform

## Problem

Design the course-authoring, enrollment, learning, and assessment domain of an online education platform.

## Required behavior

- Let instructors draft and publish versioned courses made of sections and lessons.
- Support video, text, resource, quiz, and assignment lesson types.
- Enroll eligible learners and retain the exact course version they started.
- Track lesson progress, completion, bookmarks, and resumable position.
- Grade objective quizzes and support instructor-reviewed assignments.
- Expose learner discussions and course announcements with moderation controls.
- Issue a completion record only when all configured requirements pass.

## Core model

`Course`, `CourseVersion`, `Section`, `Lesson`, `Instructor`, `Learner`, `Enrollment`, `Progress`, `Assessment`, `Attempt`, `Submission`, and `Certificate`.

Published course versions should be immutable. Content delivery, media storage, payment, and notification belong behind adapters.

## Invariants and edge cases

- One learner has at most one active enrollment per course offering.
- Attempt limits, deadlines, passing scores, and prerequisite rules are explicit policies.
- Progress updates are monotonic unless an authorized reset occurs.
- Handle course revision, revoked enrollment, concurrent playback updates, late submission, and regrading.

## Design exercise

Demonstrate publishing, enrollment, progress, quiz attempts, assignment review, course revision without corrupting existing learners, and certificate eligibility.
