import type { PartType } from './partType';

export interface Part {
    id: number;
    type: PartType;
    weight: number;
    animalRegistrationNumber: string;
    trayId: number;
    productId: number | null;
}
