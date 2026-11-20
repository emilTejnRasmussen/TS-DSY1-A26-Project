import { API_BASE_URL } from './config';

export class ApiError extends Error {
    readonly status: number;

    constructor(status: number, message: string) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
    }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    let response: Response;
    try {
        const timeout = AbortSignal.timeout(15000);
        const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout;
        response = await fetch(`${API_BASE_URL}${path}`, { ...options, signal });
    } catch (error) {
        if (options.signal?.aborted) throw error;
        if (error instanceof DOMException && error.name === 'TimeoutError') {
            throw new Error('The request timed out. Refresh to check whether the operation completed before trying again.', { cause: error });
        }
        throw new Error('Unable to reach the station service. Check the connection and try again.', { cause: error });
    }

    if (!response.ok) {
        const problem = await response.json().catch(() => null) as {
            detail?: string;
            title?: string;
            errors?: Record<string, string[]>;
        } | null;
        const fields = Object.entries(problem?.errors ?? {})
            .map(([field, messages]) => `${field}: ${messages.join(', ')}`).join('. ');
        const fallback = response.status >= 500
            ? `The station service is unavailable (${response.status}). Please try again later.`
            : `The request failed (${response.status}).`;
        const detail = problem?.detail || problem?.title || fallback;
        throw new ApiError(response.status, fields ? `${detail}. ${fields}` : detail);
    }

    return response.json() as Promise<T>;
}

export function errorMessage(error: unknown): string {
    return error instanceof Error ? error.message : 'An unexpected error occurred. Please try again.';
}
