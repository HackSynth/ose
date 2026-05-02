import { NextRequest } from 'next/server';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { middleware } from '@/middleware';

vi.mock('next-auth/jwt', () => ({
  getToken: vi.fn(),
}));

import { getToken } from 'next-auth/jwt';

const mockGetToken = vi.mocked(getToken);

function makeRequest(path: string): NextRequest {
  return new NextRequest(`http://localhost:3000${path}`);
}

beforeEach(() => {
  mockGetToken.mockReset();
});

describe('middleware — protected routes', () => {
  it('redirects unauthenticated user to /login with callbackUrl', async () => {
    mockGetToken.mockResolvedValue(null);
    const response = await middleware(makeRequest('/dashboard'));
    expect(response.status).toBe(307);
    const location = new URL(response.headers.get('location')!);
    expect(location.pathname).toBe('/login');
    expect(location.searchParams.get('callbackUrl')).toBe('/dashboard');
  });

  it('allows authenticated user to access protected route', async () => {
    mockGetToken.mockResolvedValue({ sub: 'user-1' } as never);
    const response = await middleware(makeRequest('/dashboard'));
    expect(response.status).toBe(200);
  });
});

describe('middleware — auth routes (/login, /register, /reset-password)', () => {
  it('redirects authenticated user away from /login to /dashboard', async () => {
    mockGetToken.mockResolvedValue({ sub: 'user-1' } as never);
    const response = await middleware(makeRequest('/login'));
    expect(response.status).toBe(307);
    expect(new URL(response.headers.get('location')!).pathname).toBe('/dashboard');
  });

  it('allows unauthenticated user to visit /login', async () => {
    mockGetToken.mockResolvedValue(null);
    const response = await middleware(makeRequest('/login'));
    expect(response.status).toBe(200);
  });

  it('redirects authenticated user away from /register to /dashboard', async () => {
    mockGetToken.mockResolvedValue({ sub: 'user-1' } as never);
    const response = await middleware(makeRequest('/register'));
    expect(response.status).toBe(307);
    expect(new URL(response.headers.get('location')!).pathname).toBe('/dashboard');
  });

  it('allows unauthenticated user to visit /reset-password/verify', async () => {
    mockGetToken.mockResolvedValue(null);
    const response = await middleware(makeRequest('/reset-password/verify'));
    expect(response.status).toBe(200);
  });

  it('redirects authenticated user away from /reset-password to /dashboard', async () => {
    mockGetToken.mockResolvedValue({ sub: 'user-1' } as never);
    const response = await middleware(makeRequest('/reset-password'));
    expect(response.status).toBe(307);
    expect(new URL(response.headers.get('location')!).pathname).toBe('/dashboard');
  });

  it('redirects authenticated user away from /reset-password/verify to /dashboard', async () => {
    mockGetToken.mockResolvedValue({ sub: 'user-1' } as never);
    const response = await middleware(makeRequest('/reset-password/verify'));
    expect(response.status).toBe(307);
    expect(new URL(response.headers.get('location')!).pathname).toBe('/dashboard');
  });
});
