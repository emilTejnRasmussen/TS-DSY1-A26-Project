import { API_BASE_URL } from './config';
import type { Animal, RegisterAnimal } from '../types/animal';
import type { Page } from '../types/pagination';

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