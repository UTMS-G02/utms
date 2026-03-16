# UTMS — Undergraduate Transfer Management System

A web-based management system for digitizing inter-institutional transfer applications.
**Izmir Institute of Technology — Software Engineering — Group 02**

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5, Java 21 |
| Database | PostgreSQL |
| Frontend | React + Ant Design *(upcoming)* |
| Architecture | Three-Tier (Presentation / Business Logic / Data Access) |

---

## Project Structure

```
utms/
├── backend/        # Spring Boot application
├── docs/
│   └── uml/        # PlantUML diagrams (.puml)
└── README.md
```

---

## Setup

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 15+
- VS Code + [PlantUML extension](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml) *(for UML preview)*

### Clone the Repository

```bash
git clone https://github.com/UTMS-G02/utms.git
cd utms
```

### Run the Backend

```bash
cd backend
./mvnw spring-boot:run
```

---

## Git Workflow Guide

### Branch Strategy

- **`main`** → Stable, protected branch. Never commit directly.
- **`dev`** → Integration branch. Completed features are merged here.
- **`feature/...`** → Short-lived branch for each task or feature.

### Branch Naming Convention

```
feature/your-name-short-description
```

Examples:
```
feature/arda-login-endpoint
feature/aysenur-transfer-application-entity
feature/mustafa-otp-service
```

### Daily Workflow

**1. Start every session by pulling the latest changes:**
```bash
git checkout dev
git pull origin dev
```

**2. Create a new branch for your task:**
```bash
git checkout -b feature/arda-login-endpoint
```

**3. Save your changes:**
```bash
git add .
git commit -m "short and descriptive message"
```

**4. Push your branch to GitHub:**
```bash
git push origin feature/arda-login-endpoint
```

**5. Open a Pull Request on GitHub:**
Create a PR from `feature/...` → `dev`. At least **1 team member** must review before merging.

---

### Commit Message Guidelines

Follow the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<optional scope>): <description>
```

**Types:**

| Type | When to use |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation changes |
| `refactor` | Code restructuring (no feature/fix) |
| `test` | Adding or updating tests |
| `chore` | Build config, dependencies, tooling |

**Scope** (optional) — the area of the codebase affected:
`auth`, `application`, `evaluation`, `notification`, `routing`, `docs`, etc.

**Examples:**
```
feat(auth): add OTP generation endpoint
fix(application): correct eligibility status check
docs(uml): update class diagram with yksRanking field
refactor(evaluation): extract score calculation to separate method
chore: upgrade Java version to 21
```

```
✗ "fix"
✗ "asdfgh"
✗ "WIP"
```

---

### Common Situations

**`dev` has been updated and your branch is behind:**
```bash
git checkout feature/arda-login-endpoint
git merge dev
# resolve conflicts if any, then:
git add .
git commit -m "Merge dev into feature branch"
```

**You committed to the wrong branch (before pushing):**
```bash
git reset --soft HEAD~1   # undo commit, keep changes
```

**Check what's going on:**
```bash
git status          # changed files
git log --oneline   # recent commits
git branch -a       # all branches
```

---

## Viewing UML Diagrams

1. Install the **PlantUML** extension in VS Code (`jebbs.plantuml`)
2. Open `docs/uml/class-diagram.puml`
3. Press `Alt+D` to open the preview
4. Right-click → **Export Current Diagram** → PNG/SVG
