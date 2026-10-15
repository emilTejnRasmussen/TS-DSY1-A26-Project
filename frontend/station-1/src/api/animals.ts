import { API_BASE_URL } from './config';
import type { Animal, RegisterAnimal } from '../types/animal';
import type { Page } from '../types/pagination';

export interface AnimalFilters {
    registeredDate?: string;
    farmId?: number;
    page?: number;
    size?: number;
    sort?: string;
}

export async function getAnimals(
    filters: AnimalFilters = {},
    signal?: AbortSignal
): Promise<Page<Animal>> {
    const params = new URLSearchParams({
        page: String(filters.page ?? 0),
        size: String(filters.size ?? 10),
        sort: filters.sort ?? 'registeredAt,desc',
    });

    if (filters.registeredDate) {
        params.set('registeredDate', filters.registeredDate);
    }

    if (filters.farmId !== undefined) {
        params.set('farmId', String(filters.farmId));
    }

    const response = await fetch(
        `${API_BASE_URL}/animals?${params}`,
        { signal }
    );

    if (!response.ok) {
        throw new Error(`Failed to load animals (${response.status})`);
    }

    return response.json() as Promise<Page<Animal>>;
}

export async function getAnimal(
    registrationNumber: string
): Promise<Animal> {
    const response = await fetch(
        `${API_BASE_URL}/animals/${encodeURIComponent(registrationNumber)}`
    );

    if (!response.ok) {
        throw new Error(`Failed to find animal (${response.status})`);
    }

    return await response.json() as Promise<Animal>;
}

export async function registerAnimal(
    animal: RegisterAnimal
): Promise<Animal> {
    const response = await fetch(`${API_BASE_URL}/animals`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(animal)
    });

    if (!response.ok) {
        throw new Error(`Failed to register animal (${response.status})`);
    }

    return await response.json() as Promise<Animal>;
}

export async function getRecentAnimals(): Promise<Animal[]> {
    const params = new URLSearchParams({
        page: '0',
        size: '5',
        sort: 'registeredAt,desc'
    });

    const response = await fetch(`${API_BASE_URL}/animals?${params}`);

    if (!response.ok) {
        throw new Error(`Failed to load recent animals (${response.status})`);
    }

    const result: Page<Animal> = await response.json();

    return result.content;
}