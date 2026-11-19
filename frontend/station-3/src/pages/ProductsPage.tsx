import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { getProduct, getProducts } from '../api/products';
import { errorMessage } from '../api/request';
import type { Product } from '../types/product';
import type { Page } from '../types/pagination';
import Pagination from '../components/Pagination';
import PartsTable from '../components/PartsTable';
import { formatDate, formatWeight } from '../utils/format';

export default function ProductsPage() {
    const [page, setPage] = useState(0);
    const [refreshKey, setRefreshKey] = useState(0);
    const [result, setResult] = useState<Page<Product> | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [lookupId, setLookupId] = useState('');
    const [productId, setProductId] = useState<number | null>(null);
    const [product, setProduct] = useState<Product | null>(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState('');

    useEffect(() => {
        const controller = new AbortController();
        async function loadProducts() {
            setIsLoading(true);
            setError('');
            try {
                const response = await getProducts(page, 10, controller.signal);
                if (controller.signal.aborted) return;
                setResult(response);
                if (page > 0 && response.content.length === 0) setPage(Math.max(0, response.totalPages - 1));
            } catch (err) {
                if (!controller.signal.aborted) setError(errorMessage(err));
            } finally {
                if (!controller.signal.aborted) setIsLoading(false);
            }
        }
        void loadProducts();
        return () => controller.abort();
    }, [page, refreshKey]);

    useEffect(() => {
        if (productId === null) return;
        const controller = new AbortController();
        async function loadProduct() {
            setDetailLoading(true);
            setDetailError('');
            try {
                const response = await getProduct(productId!, controller.signal);
                if (!controller.signal.aborted) setProduct(response);
            } catch (err) {
                if (!controller.signal.aborted) setDetailError(errorMessage(err));
            } finally {
                if (!controller.signal.aborted) setDetailLoading(false);
            }
        }
        void loadProduct();
        return () => controller.abort();
    }, [productId, refreshKey]);

    function inspectProduct(id: number) {
        setProduct(null);
        setProductId(id);
        setDetailError('');
        if (productId === id) setRefreshKey(key => key + 1);
    }

    function handleLookup(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const id = Number(lookupId);
        if (Number.isSafeInteger(id) && id > 0) inspectProduct(id);
    }

    return (
        <section>
            <div className="section-heading"><h2>Products</h2><p>Browse packaged products and their source parts.</p></div>
            <form className="toolbar inline-form" onSubmit={handleLookup}>
                <div className="field">
                    <label htmlFor="product-lookup">Product ID</label>
                    <input id="product-lookup" type="number" min="1" max={Number.MAX_SAFE_INTEGER} step="1" required value={lookupId}
                        placeholder="Enter a product ID" onChange={event => setLookupId(event.target.value)} />
                </div>
                <button type="submit" className="button-secondary" disabled={!lookupId || detailLoading}>Find product</button>
                <button type="button" className="button-secondary" disabled={isLoading}
                    onClick={() => setRefreshKey(key => key + 1)}>Refresh products</button>
            </form>

            {error && <div className="form-message error" role="alert">{error}</div>}
            <div className="table-wrapper"><table>
                <thead><tr><th>Product ID</th><th>Name</th><th>Total weight</th><th>Parts</th><th>Created at</th><th>Details</th></tr></thead>
                <tbody>{isLoading || error ? <tr><td colSpan={6} className="empty-state">
                    {isLoading ? 'Loading products...' : 'Products could not be loaded.'}
                </td></tr> : !result?.content.length ? <tr><td colSpan={6} className="empty-state">No products have been created yet.</td></tr>
                    : result.content.map(item => <tr key={item.id}>
                        <td>{item.id}</td><td>{item.name}</td><td>{formatWeight(item.totalWeight)}</td>
                        <td>{item.parts.length}</td><td>{formatDate(item.createdAt)}</td>
                        <td><button type="button" className="button-secondary" aria-expanded={productId === item.id}
                            onClick={() => inspectProduct(item.id)}>View product</button></td>
                    </tr>)}</tbody>
            </table></div>
            {!error && result && <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements}
                onPageChange={setPage} isLoading={isLoading} label="products" />}

            {productId !== null && <section className="detail-panel" aria-label={`Product ${productId} details`}>
                <div className="detail-heading"><h3>Product {productId}</h3>
                    <button type="button" className="button-secondary" onClick={() => { setProductId(null); setProduct(null); }}>Close</button>
                </div>
                {detailLoading ? <p className="muted" role="status">Loading product details...</p>
                    : detailError ? <div className="form-message error" role="alert">{detailError}</div>
                        : product?.id === productId && <>
                            <div className="detail-grid">
                                <div className="detail-item"><span>Name</span><strong>{product.name}</strong></div>
                                <div className="detail-item"><span>Total weight</span><strong>{formatWeight(product.totalWeight)}</strong></div>
                                <div className="detail-item"><span>Source trays</span><strong>{[...new Set(product.parts.map(part => part.trayId))].join(', ')}</strong></div>
                                <div className="detail-item"><span>Created at</span><strong>{formatDate(product.createdAt)}</strong></div>
                            </div>
                            <PartsTable parts={product.parts} />
                        </>}
            </section>}
        </section>
    );
}
