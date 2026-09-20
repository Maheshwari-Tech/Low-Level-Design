# Stack Overflow-Like System

## Problem

Design a question-and-answer platform where registered users ask tagged questions, answer and comment, vote on content, accept an answer, search, and build reputation.

## Requirements

- Register and authenticate users.
- Create questions with title, body, tags, and lifecycle state.
- Add answers and comments.
- Allow a question owner to accept one answer.
- Upvote or downvote content at most once per user, with vote changes handled consistently.
- Adjust reputation and award badges from explicit rules.
- Search by text, tag, author, and status.
- Rank recent, active, or trending questions.

## Core Model

- **`User`**: identity, credentials boundary, reputation, and badges.
- **`Question`**: author, content, tags, answers, accepted answer, votes, and status.
- **`Answer`**: author, body, comments, votes, and accepted flag.
- **`Comment`**: lightweight response attached to a question or answer.
- **`Tag`**: normalized category and usage metadata.
- **`Vote`**: voter, target, value, and timestamp.
- **`SearchService`**: query and ranking boundary.
- **`StackOverflowService`**: coordinates writes and authorization rules.

## Data Structures and Invariants

- `Map<String, User>` and `Map<String, Question>` for entity lookup.
- `Map<QuestionId, List<Answer>>` or answer IDs stored on the question.
- `Map<String, Tag>` plus a tag-to-question inverted index.
- `Map<(voterId, targetId), Vote>` to enforce one active vote per user and target.
- A priority queue or precomputed score index for trending questions.

Only the question owner may accept an answer; an answer must belong to that question; one target cannot receive duplicate votes from the same user; reputation updates and vote persistence must commit together.

## Patterns

- **Observer** for reputation, badge, and notification reactions to votes or accepted answers.
- **Strategy** for search ranking and reputation rules.
- **Factory** for validated creation of questions, answers, comments, and votes.
- **State** can guard open, closed, duplicate, and deleted question transitions.

## Implementation Status

This target currently contains the design document only. Authentication, content storage, voting, comments, acceptance, reputation, badges, search, trending, persistence, moderation, and notifications are design requirements and are not implemented here.
