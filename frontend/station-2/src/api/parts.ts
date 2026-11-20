import { request } from './request';
import type { Part, RegisterPart } from '../types/part';
import type { PartType } from '../types/partType';
import type { Page } from '../types/pagination';

export interface PartFilters {
    animalRegistrationNumber?: string;
    partType?: PartType;
    page?: number;
    size?: number;
    sort?: string;
}

export function getParts(filters: PartFilters = {}, signal?: AbortSignal): Promise<Page<Part>> {
    const params = new URLSearchParams({
        page: String(filters.page ?? 0),
        size: String(filters.size ?? 10),
        sort: filters.sort ?? 'registeredAt,desc',
    });
    if (filters.animalRegistrationNumber) params.set('animalRegistrationNumber', filters.animalRegistrationNumber);
    if (filters.partType) params.set('partType', filters.partType);
    return request(`/parts?${params}`, { signal });
}

export function getPart(id: number, signal?: AbortSignal): Promise<Part> {
    return request(`/parts/${id}`, { signal });
}

export function registerPart(part: RegisterPart): Promise<Part> {
    return request('/parts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(part),
    });
}
