import type { PartType } from './partType';

export interface Part {
    id: number;
    weight: number;
    type: PartType;
    animalRegistrationNumber: string;
    trayId: number | null;
    registeredAt?: string;
}

export interface RegisterPart {
    animalRegistrationNumber: string;
    type: PartType;
    weight: number;
}
