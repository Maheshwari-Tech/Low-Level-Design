# Task Management System

## Problem

Design a Jira/Trello-like service that organizes tasks into projects, assigns work to users, enforces workflow transitions, and supports collaboration and prioritization.

## Requirements

- Register users and assign project roles.
- Create projects and tasks with title, description, priority, due date, and assignee.
- Move tasks through `TODO`, `IN_PROGRESS`, and `DONE` using valid transitions.
- Reassign, reprioritize, and reschedule tasks.
- Add comments and retain an activity history.
- Query by project, assignee, status, priority, or overdue state.
- Notify interested users about material changes.
- Support replaceable task-assignment policies.

## Core Model

- **`TaskManager`**: use-case facade and transaction boundary.
- **`Task`**: task data, assignee, priority, due date, and current status.
- **`Project`**: membership, workflow, and related task IDs.
- **`User`**: identity and role assignments.
- **`Workflow`**: allowed states and transitions.
- **`Comment`**: author, body, and timestamp.
- **`Activity`**: immutable audit entry for task changes.
- **`AssignmentStrategy`**: manual, round-robin, workload, or skill-based assignment.

## Data Structures and Invariants

- `Map<String, Task>` and `Map<String, Project>` for primary lookup.
- Secondary sets/indexes by project, user, status, and priority.
- A priority queue for a work queue ordered by priority and due date.
- Append-only comment and activity lists.

A task belongs to one project; only project members with sufficient permission may mutate it; state changes must be accepted by the project's workflow; the audit event must commit with the change.

## Patterns

- **State** or an explicit transition table for workflow rules.
- **Observer** for assignment, comment, due-date, and status notifications.
- **Factory** for task creation with defaults and validation.
- **Strategy** for assignment and prioritization policies.

## Task Flow

1. Authorize the actor against the project and requested action.
2. Validate fields, assignment, dates, and workflow transition.
3. Persist the task change and matching activity record atomically.
4. Update query indexes.
5. Notify watchers after commit.

## Implementation Status

This target currently contains the design document only. Tasks, projects, workflows, comments, role checks, notifications, assignment strategies, indexes, and persistence are not implemented here.
