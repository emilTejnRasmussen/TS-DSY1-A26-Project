import type { Part } from './part';

export interface Product {
    id: number;
    name: string;
    totalWeight: number;
    parts: Part[];
    createdAt: string;
}

export interface CreateProduct {
    name: string;
    partIds: number[];
}
