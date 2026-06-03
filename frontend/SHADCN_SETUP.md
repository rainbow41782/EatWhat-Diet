# shadcn / Tailwind / TypeScript Setup Notes

This project already uses TypeScript and Next.js. It does **not** currently ship with a Tailwind config file, so if your teammates want to use the component exactly as copied below, they should add Tailwind and shadcn defaults first.

Default paths used by shadcn conventions in this repo
- Components: `frontend/components/ui`
- Shared utilities: `frontend/lib`
- Global styles: `frontend/styles/globals.css`

Why `components/ui` matters
- shadcn CLI expects reusable UI primitives in a dedicated `components/ui` folder.
- Keeping third-party UI primitives there prevents mixing app-specific pages with low-level UI building blocks.
- It also makes imports like `@/components/ui/sign-in-card-2` predictable and easier to refactor.

If the project does not have Tailwind yet, add it first

```bash
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

Then update `tailwind.config.ts` content paths to include your Next pages and components (for example `./pages/**/*.{ts,tsx}`, `./components/**/*.{ts,tsx}`, `./lib/**/*.{ts,tsx}`).

Add shadcn CLI metadata

```bash
npx shadcn@latest init
```

Recommended answers / settings
- `components` path: `components`
- `ui` path: `components/ui`
- `utils` path: `lib/utils`
- CSS file: `styles/globals.css`
- TypeScript: `yes`

After that, paste `components/ui/sign-in-card-2.tsx` and import it from `@/components/ui/sign-in-card-2`.
