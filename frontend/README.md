# Frontend (Development & Packaging)

This folder contains the Next.js frontend used during development. Use this README to run locally or package assets for sharing.

Development

1. Install dependencies:

```bash
cd frontend
npm install
```

2. Start dev server:

```bash
npm run dev
# Default: http://localhost:3000
```

Build & Export (static)

```bash
npm run build
npx next export -o out
# Result will be in frontend/out
```

Packaging (zip)

From project root you can run the included PowerShell helper to build and package both the exported site and the static Spring Boot page:

```powershell
.
\scripts\package_frontend.ps1
```

This will generate `frontend-build.zip` (exported Next.js out) and `static-login.zip` (files under `src/main/resources/static`).
