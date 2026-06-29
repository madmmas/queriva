declare module '*.css' {
  const content: string;
  export default content;
}

declare module 'jest-axe' {
  export function axe(container: Element): Promise<{ violations: unknown[] }>;
  export const toHaveNoViolations: unknown;
}

declare module 'vitest' {
  interface Assertion<T = unknown> {
    toHaveNoViolations(): T;
  }
  interface AsymmetricMatchersContaining {
    toHaveNoViolations(): unknown;
  }
}
