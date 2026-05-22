import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Auth pages: client-side only (forms with state, no benefit from SSR)
  {
    path: 'auth/**',
    renderMode: RenderMode.Client,
  },
  // Protected dashboard routes: must be client-only so localStorage is
  // available when the auth guard runs. SSR has no session/token context,
  // which causes a F5 logout when routes are prerendered.
  {
    path: 'dashboard/**',
    renderMode: RenderMode.Client,
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
