export const ANIMAL_TYPES = [
    'PIG',
    'COW',
    'GOAT',
    'SHEEP',
    'CHICKEN',
    'TURKEY',
    'DUCK',
    'GOOSE'
] as const;

export type AnimalType = typeof ANIMAL_TYPES[number];

export interface RegisterAnimal {
    type: AnimalType;
    weight: number;
    farmId: number;
}

export interface Animal {
    registrationNumber: string;
    type: AnimalType;
    registeredAt: string;
    weight: number;
    farmId: number;
    farmName: string;
}