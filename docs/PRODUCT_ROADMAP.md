# RepoMind product roadmap

RepoMind should help a developer who is new to a large codebase answer three questions quickly: **where does this behavior live, how does it work across layers, and what else could it affect?**

## Feature sequence

| Order | Feature | Developer problem it solves |
|---|---|---|
| 1 | **Feature Finder** *(built first)* | Search for behavior in plain language and see ranked code locations, line ranges, and why each location matched. |
| 2 | **Request-to-code tracing** | Follow a user action from UI event through API route, service, database, and response; show a clickable sequence diagram. |
| 3 | **Feature maps and dependency graph** | See modules, entry points, imports, services, and connections in a navigable architecture graph. |
| 4 | **Change impact radar** | Select a file or symbol and find callers, dependent modules, affected routes, and tests before editing. |
| 5 | **New developer guided tour** | Generate a repo-specific first-hour path with five files to read, key concepts, glossary, and starter tasks. |
| 6 | **Test-to-feature coverage map** | Connect a feature to tests that exercise it, identify gaps, and explain how to run those tests. |
| 7 | **Bug investigation workspace** | Describe an observed failure and trace likely code paths, error handling, logs, and relevant tests with evidence. |
| 8 | **API explorer** | Extract routes, request/response types, auth requirements, and link each endpoint to its handler and service. |
| 9 | **Data lineage explorer** | Trace a field from UI input through validation and persistence to its readers and outputs. |
| 10 | **Safe edit plan** | Turn a requested feature into a checklist of files, behavior, edge cases, and tests, with citations for every claim. |
| 11 | **PR impact review** | Compare a branch diff against repository context, flag missed callers/tests, and explain behavior changes. |
| 12 | **Repository comparison** | Compare two repos or versions for architecture, feature ownership, API differences, and migration risk. |
| 13 | **Docs drift detector** | Find README/API docs that disagree with the implementation and point to both sides of the mismatch. |
| 14 | **Dependency and security explorer** | Explain where packages are used, what features depend on them, and surface vulnerable or abandoned dependencies. |
| 15 | **Team knowledge layer** | Add reviewed annotations, ownership, decisions, and architecture notes that stay attached to code locations. |
| 16 | **Multi-repository workspace** | Search shared libraries and cross-service flows across a whole product instead of one repository at a time. |
| 17 | **Branch and commit awareness** | Pin answers to a commit, compare releases, and keep links correct when the default branch changes. |
| 18 | **Production-ready collaboration** | Accounts, private repositories, access controls, audit history, rate limits, background queues, and incremental indexing. |

## Build order

Start with the capabilities that directly reduce time spent onboarding and hunting for code: feature discovery, end-to-end flow tracing, impact analysis, and guided tours. Add graph and test intelligence after symbol/reference indexing exists. Build enterprise and team features after repository processing is durable and access-controlled.

The initial release is an open, shared public workspace with no sign-in. Repository data is keyed by normalized public GitHub URL. Authentication, per-user history, private bookmarks, and private repositories are later roadmap items.

## Feature Finder — delivered scope

- Repository overview with file and code-section totals.
- Language breakdown and quick repository-area map.
- Likely application entry points with GitHub line links.
- Plain-language feature search with suggested topics.
- Ranked code hits that include source snippets, paths, lines, and a short match explanation.
- One-click handoff from a discovered feature to a grounded chat question.
- **Request trace mode** groups likely matches into UI, API, business logic, and data layers.
- **Persistent search history** stores the query, discovery mode, result count, and timestamp in PostgreSQL and lets developers resume a search.
- **Saved code shortlist** bookmarks exact source ranges in PostgreSQL and links back to the repository lines.
- Works with keyword search when vector credentials are unavailable; semantic ranking is added when Gemini and Pinecone are configured.
