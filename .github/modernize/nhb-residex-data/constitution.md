<!--
Sync Impact Report
Version change: none -> 1.0.0 (initial baseline; no prior constitution exists)
Modified principles: None
Added principles:
- Explicit Boundaries and Scoped Delivery
- Stable Data Contracts and Provenance
- Testable by Default
- Explicit Failures and Safe Publishing
- Accessible, Consistent, Minimal UI
Added sections: Data and Deployment Constraints; Development Workflow and Quality Gates
Removed sections: Target Technology Stack (not applicable to incremental UPGRADE mode)
Templates requiring updates:
- ✅ skills/creating-implementation-plan/templates/plan-template.md — reviewed; no changes needed.
- ✅ skills/feature-inventory/templates/spec-template.md — reviewed; already references constitution.md.
- ✅ skills/creating-implementation-plan/templates/tasks-template.md — reviewed; no changes needed.
- ⚠ skills/breaking-down-tasks/templates/tasks-template.md — unavailable in the active skills directory;
  the implementation-plan task template was reviewed as the available equivalent.
Follow-up TODO: Confirm the original ratification date.
-->

# Chirag Raval Financial Data Lab Constitution

## Migration Mode

**Mode**: UPGRADE

**Justification**: This repository is an existing personal website and financial
analytics project. New capabilities are additive and must preserve its current
architecture, applications, data, and deployment behavior.

## Core Principles

### I. Explicit Boundaries and Scoped Delivery

Keep application assets, pipeline code, generated data, shared UI, and
deployment responsibilities in their documented paths. Build each application
independently. A feature's build and deployment MUST affect only its owned
artifact paths; unrelated applications, datasets, and published content MUST
remain intact. This keeps changes reviewable and prevents one feature from
disrupting another.

### II. Stable Data Contracts and Provenance

Every generated dataset MUST have a documented schema and identify the
meaning, units, dates, and source of its values. Keep test and production data
separate. Transformations MUST preserve relevant source metadata and
precedence; time-series comparisons MUST use explicit date-alignment rules
without looking ahead or inventing observations. Missing, invalid, or partial
data MUST NOT be represented as valid zero values. Stable input MUST produce
unchanged canonical output.

### III. Testable by Default

New data parsing, normalization, merging, and comparison behavior MUST have
focused automated checks using controlled fixtures where practical. Tests
MUST cover relevant boundaries such as missing fields, overlapping periods,
corrections, and date alignment. Validation MUST avoid production data paths
and unintended publication. Use the existing build and test tooling rather
than introducing tooling without a demonstrated need.

### IV. Explicit Failures and Safe Publishing

Pipelines MUST report invalid responses and incomplete required inputs
explicitly; they MUST NOT silently substitute fabricated or success-shaped
fallback data. Pipelines write artifacts only and MUST NOT perform Git or
deployment operations. Production collection and publishing require the
repository's explicit gates. Publishing MUST validate required artifacts,
write only the selected paths, and preserve unrelated site content.

### V. Accessible, Consistent, Minimal UI

New user-facing pages MUST reuse the shared site shell, route configuration,
and design tokens where applicable. Core interactions MUST support keyboard
use, visible focus, accessible labels, and narrow-screen layouts. Charts and
tables MUST label dates, units, and relevant data limitations. Add navigation,
dependencies, or architectural layers only when required by the approved
feature scope.

## Data and Deployment Constraints

- Preserve the existing Java 25/Maven pipeline, React 18/Vite applications,
  static JSON interfaces, shared site shell, and GitHub Pages deployment
  boundaries unless a separately approved change updates this architecture.
- Place application code under `apps/<app-id>/`, pipeline code under
  `pipelines/<pipeline-id>/`, and generated datasets under
  `data/<dataset-id>/{test,prod}/`.
- Build site and data artifacts independently. Build and scrape jobs MUST NOT
  receive deployment write permissions.
- Reuse the serialized publisher and exact path scopes; do not clear or
  replace the Pages root or another application's content.

## Development Workflow and Quality Gates

- The feature plan remains the source of truth for that feature's scope,
  decisions, status, validation, and handoff. This constitution defines
  repository-wide constraints and does not replace feature-specific plans.
- Plans MUST identify applicable principles and document any proposed
  deviation with its rationale before implementation.
- Validate the smallest relevant build and test targets. Before production
  publication, validate the artifact schema and deployment path scope.
- Update directly related documentation when behavior, data contracts, or
  operational steps change.
- Review changes for requirement coverage, data correctness, explicit error
  handling, accessibility, and preservation of unrelated routes and data.

## Governance

This constitution establishes non-negotiable repository constraints.
Amendments require a reviewed change that records the reason, affected
principles, and downstream documentation or template impact. Use semantic
versioning: MAJOR for incompatible principle changes, MINOR for new or
materially expanded principles, and PATCH for non-semantic clarification.

Every feature plan and implementation review MUST check applicable principles.
Deviations require explicit rationale and approval in the feature plan; they
MUST NOT be introduced silently. Reviewers verify the relevant tests, artifact
contracts, user-facing accessibility, and deployment boundaries.

Use [architecture.md](../../../docs/architecture.md) and
[adding-apps-and-data.md](../../../docs/adding-apps-and-data.md) for the
repository's architecture and implementation conventions.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): confirm original adoption date | **Last Amended**: 2026-11-23
