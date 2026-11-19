import type { Part } from './part';
import type { PartType } from './partType';

export interface Tray {
    id: number;
    type: PartType;
    totalWeight: number;
    availableWeight: number;
    parts: Part[];
    sealedAt: string;
    receivedAt: string;
}
