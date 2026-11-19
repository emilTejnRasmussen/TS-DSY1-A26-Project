import type { PartType } from './partType';

export type TrayStatus = 'OPEN' | 'SEALED';

export interface Tray {
    id: number;
    type: PartType;
    status: TrayStatus;
    maxWeight: number;
    currentWeight: number;
    partIds: number[];
    createdAt: string;
    sealedAt: string | null;
}

export interface CreateTray {
    type: PartType;
    maxWeight: number;
}
