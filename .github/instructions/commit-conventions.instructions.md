---
description: "Use when writing, reviewing, or suggesting git commit messages. Enforces Conventional Commits format without scopes."
---

# Commit Message Conventions

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.

## Format

```
<type>: <short description>

[optional body]

[optional footer(s)]
```

## Allowed Types

| Type       | When to use                                            |
| ---------- | ------------------------------------------------------ |
| `feat`     | A new feature visible to users or consumers            |
| `fix`      | A bug fix                                              |
| `refactor` | Code change that is neither a feature nor a fix        |
| `test`     | Adding or updating tests                               |
| `docs`     | Documentation only changes                             |
| `chore`    | Maintenance tasks: dependencies, build config, tooling |
| `ci`       | CI/CD pipeline changes                                 |
| `perf`     | Performance improvements                               |
| `style`    | Formatting, whitespace — no logic change               |

## Rules

- **No scopes** — omit the `(scope)` part entirely (e.g., use `feat:` not `feat(api):`)
- Subject line: lowercase, no trailing period, ≤72 characters
- Use the imperative mood: "add feature" not "added feature" or "adds feature"
- Breaking changes: append `!` after the type (`feat!:`) and explain in the footer with `BREAKING CHANGE:`

## Examples

```
feat: add percentage calculation endpoint
fix: return 429 when rate limit is exceeded
refactor: extract audit log filter into separate class
test: add integration tests for calculation service
chore: upgrade spring boot to 4.0.1
docs: document hexagonal architecture layers
feat!: change calculation response format

BREAKING CHANGE: response now returns `result` instead of `value`
```

## Workflow

- **Never execute `git commit` automatically.** Always present the proposed commit message to the user and wait for explicit confirmation before running any git command.
- Suggest the commit command as a code block for the user to run manually, e.g.:
  ```
  git add -A
  git commit -m "type: description"
  ```

## Anti-patterns

- `fix: fixed the bug` — past tense, vague
- `feat(api): add endpoint` — scopes are not used in this project
- `WIP` / `misc` / `update` — not a valid type, not descriptive
- Merging multiple unrelated changes into one commit
