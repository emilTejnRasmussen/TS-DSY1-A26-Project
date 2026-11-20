import { request } from './request';
import type { Page } from '../types/pagination';
import type { PartType } from '../types/partType';
import type { Tray } from '../types/tray';

export interface TrayFilters {
    type?: PartType;
    availableOnly?: boolean;
    page?: number;
    size?: number;
}

export function getTrays(filters: TrayFilters = {}, signal?: AbortSignal): Promise<Page<Tray>> {
    const params = new URLSearchParams({
        page: String(filters.page ?? 0),
        size: String(filters.size ?? 10),
        sort: 'receivedAt,desc',
        availableOnly: String(filters.availableOnly ?? true),
    });
    if (filters.type) params.set('type', filters.type);
    return request<Page<Tray>>(`/trays?${params}`, { signal });
}

export function getTray(id: number, signal?: AbortSignal): Promise<Tray> {
    return request<Tray>(`/trays/${id}`, { signal });
}
