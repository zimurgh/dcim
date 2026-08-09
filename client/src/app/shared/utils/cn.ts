import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

/** Spartan/helm className helper. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
