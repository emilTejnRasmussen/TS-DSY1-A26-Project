
import { API_BASE_URL } from './config';
import type { Farm } from '../types/farm';
import type { Page } from '../types/pagination';

export async function getFarms(
    nameContains: string = '',
    page: number = 0,
    size: number = 10,
    signal?: AbortSignal
): Promise<Page<Farm>> {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort: 'name,asc'
    });

    if (nameContains.trim()) {
        params.set('nameContains', nameContains.trim());
    }

    const response = await fetch(`${API_BASE_URL}/farms?${params}`, { signal });

    if (!response.ok) {
        throw new Error(`Failed to load farms (${response.status})`);
    }

    return response.json() as Promise<Page<Farm>>;
}

export async function searchFarms(
    nameContains: string,
    signal?: AbortSignal
): Promise<Farm[]> {
    const result = await getFarms(nameContains, 0, 10, signal);

    return result.content;
}

export async function createFarm(name: string): Promise<Farm> {
    const response = await fetch(`${API_BASE_URL}/farms`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name })
    });

    if (!response.ok) {
        throw new Error(`Failed to create farm (${response.status})`);
    }

    return response.json() as Promise<Farm>;
}
