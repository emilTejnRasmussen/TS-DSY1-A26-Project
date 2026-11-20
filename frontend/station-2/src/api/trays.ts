import { request } from './request';
import type { CreateTray, Tray, TrayStatus } from '../types/tray';
import type { PartType } from '../types/partType';
import type { Page } from '../types/pagination';

export interface TrayFilters {
    type?: PartType;
    status?: TrayStatus;
    page?: number;
    size?: number;
}

export function getTrays(filters: TrayFilters = {}, signal?: AbortSignal): Promise<Page<Tray>> {
    const params = new URLSearchParams({
        page: String(filters.page ?? 0),
        size: String(filters.size ?? 10),
        sort: 'createdAt,desc',
    });
    if (filters.type) params.set('type', filters.type);
    if (filters.status) params.set('status', filters.status);
    return request(`/trays?${params}`, { signal });
}

export function getTray(id: number, signal?: AbortSignal): Promise<Tray> {
    return request(`/trays/${id}`, { signal });
}

export function createTray(tray: CreateTray): Promise<Tray> {
    return request('/trays', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(tray),
    });
}

export function addPart(trayId: number, partId: number): Promise<Tray> {
    return request(`/trays/${trayId}/parts/${partId}`, { method: 'PUT' });
}

export function removePart(trayId: number, partId: number): Promise<Tray> {
    return request(`/trays/${trayId}/parts/${partId}`, { method: 'DELETE' });
}

export function sealTray(trayId: number): Promise<Tray> {
    return request(`/trays/${trayId}/seal`, { method: 'PUT' });
}
