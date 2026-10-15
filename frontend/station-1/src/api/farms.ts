import { API_BASE_URL } from './config';
import type { Farm } from '../types/farm';
import type { Page } from '../types/pagination';

export async function searchFarms(
    nameContains: string,
    signal?: AbortSignal
): Promise<Farm[]> {
    const params = new URLSearchParams({
        nameContains,
        size: '10',
        sort: 'name,asc'
    });

    const response = await fetch(
        `${API_BASE_URL}/farms?${params}`,
        { signal }
    );

    if (!response.ok) {
        throw new Error(`Failed to search farms: ${response.status}`);
    }

    const result: Page<Farm> = await response.json();
    return result.content;
}