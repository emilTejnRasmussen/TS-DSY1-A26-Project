import { useState } from 'react';
import type { FormEvent } from 'react';
import { createProduct } from '../api/products';
import { getTray } from '../api/trays';
import { ApiError, errorMessage } from '../api/request';
import InventoryBrowser from '../components/InventoryBrowser';
import PartsTable from '../components/PartsTable';
import type { Part } from '../types/part';
import type { Product } from '../types/product';
import type { Tray } from '../types/tray';
import { formatWeight, totalWeight } from '../utils/format';

interface CreateProductPageProps {
    selectedParts: Part[];
    onTogglePart: (part: Part) => void;
    onSelectionChange: (parts: Part[]) => void;
    onReconcileTrays: (trays: Tray[]) => void;
    refreshKey: number;
    onRefresh: () => void;
    onProductCreated: () => void;
    isBusy: boolean;
    onBusyChange: (busy: boolean) => void;
}

export default function CreateProductPage({ selectedParts, onTogglePart, onSelectionChange,
    onReconcileTrays, refreshKey, onRefresh, onProductCreated, isBusy, onBusyChange }: CreateProductPageProps) {
    const [name, setName] = useState('');
    const [error, setError] = useState('');
    const [selectionError, setSelectionError] = useState('');
    const [createdProduct, setCreatedProduct] = useState<Product | null>(null);

    async function refreshSelection(): Promise<boolean> {
        const trayIds = [...new Set(selectedParts.map(part => part.trayId))];
        const results = await Promise.allSettled(trayIds.map(id => getTray(id)));
        const refreshedParts: Part[] = [];
        let failed = false;
        for (const part of selectedParts) {
            const result = results[trayIds.indexOf(part.trayId)];
            if (result.status === 'fulfilled') {
                const refreshed = result.value.parts.find(item => item.id === part.id && item.productId === null);
                if (refreshed) refreshedParts.push(refreshed);
            } else if (!(result.reason instanceof ApiError && result.reason.status === 404)) {
                failed = true;
                refreshedParts.push(part);
            }
        }
        onSelectionChange(refreshedParts);
        setSelectionError(failed
            ? 'Some selected inventory could not be checked. Refresh the selected parts before creating a product.' : '');
        return !failed;
    }

    async function handleRefreshSelection() {
        if (isBusy) return;
        onBusyChange(true);
        try {
            const refreshed = await refreshSelection();
            if (refreshed) setError('');
            onRefresh();
        } finally {
            onBusyChange(false);
        }
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (isBusy || selectionError) return;
        if (!name.trim() || selectedParts.length === 0) {
            setError('Enter a product name and select at least one available part.');
            return;
        }
        onBusyChange(true);
        setError('');
        setCreatedProduct(null);
        try {
            const product = await createProduct({ name: name.trim(), partIds: selectedParts.map(part => part.id) });
            setCreatedProduct(product);
            setName('');
            onProductCreated();
        } catch (err) {
            const message = errorMessage(err);
            if (err instanceof ApiError && (err.status === 409 || err.status === 404)) {
                const refreshed = await refreshSelection();
                setError(refreshed ? `${message} The selection has been refreshed; review it before trying again.` : message);
                onRefresh();
            } else {
                setError(message);
            }
        } finally {
            onBusyChange(false);
        }
    }

    return (
        <section>
            <div className="section-heading">
                <h2>Create product</h2>
                <p>Combine available parts from one or more trays. Matching and mixed part types are supported.</p>
            </div>

            {createdProduct && <div className="form-message success" role="status">
                <strong>Product {createdProduct.id} created successfully.</strong>
                <span>{createdProduct.name} · {formatWeight(createdProduct.totalWeight)}</span>
            </div>}

            <form className="form-panel" onSubmit={handleSubmit}>
                <div className="field">
                    <label htmlFor="product-name">Product name</label>
                    <input id="product-name" value={name} maxLength={255} required disabled={isBusy}
                        placeholder="Enter a product name" onChange={event => setName(event.target.value)} />
                </div>
                <div className="detail-heading">
                    <h3>Selected parts</h3>
                    <span>{selectedParts.length} parts · {formatWeight(totalWeight(selectedParts))}</span>
                </div>
                {selectedParts.length > 0 ? <PartsTable parts={selectedParts} selectedParts={selectedParts}
                    onToggle={onTogglePart} disabled={isBusy} />
                    : <p className="muted">Choose a tray below and select the parts to package.</p>}

                {error && <div className="form-message error" role="alert">{error}</div>}
                {selectionError && <div className="form-message error" role="alert">{selectionError}</div>}
                <div className="toolbar">
                    <div className="actions">
                        <button type="button" className="button-secondary" disabled={isBusy || selectedParts.length === 0}
                            onClick={() => { onSelectionChange([]); setSelectionError(''); }}>Clear selection</button>
                        <button type="button" className="button-secondary" disabled={isBusy || selectedParts.length === 0}
                            onClick={() => void handleRefreshSelection()}>Refresh selected parts</button>
                    </div>
                    <button type="submit" className="button-primary"
                        disabled={isBusy || !!selectionError || !name.trim() || selectedParts.length === 0}>
                        {isBusy ? 'Please wait...' : 'Create product'}
                    </button>
                </div>
            </form>

            <section className="section-divider">
                <div className="section-heading"><h3>Available inventory</h3>
                    <p>Your selection is kept when you change pages or browse another tray.</p></div>
                <InventoryBrowser selectedParts={selectedParts} onTogglePart={onTogglePart}
                    onReconcileTrays={onReconcileTrays} refreshKey={refreshKey} onRefresh={onRefresh} disabled={isBusy} />
            </section>
        </section>
    );
}
