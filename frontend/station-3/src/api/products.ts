import { request } from './request';
import type { Page } from '../types/pagination';
import type { CreateProduct, Product } from '../types/product';

export function getProducts(page = 0, size = 10, signal?: AbortSignal): Promise<Page<Product>> {
    const params = new URLSearchParams({ page: String(page), size: String(size), sort: 'createdAt,desc' });
    return request<Page<Product>>(`/products?${params}`, { signal });
}

export function getProduct(id: number, signal?: AbortSignal): Promise<Product> {
    return request<Product>(`/products/${id}`, { signal });
}

export function createProduct(product: CreateProduct): Promise<Product> {
    return request<Product>('/products', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(product),
    });
}
