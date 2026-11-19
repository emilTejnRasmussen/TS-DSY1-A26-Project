import { useEffect, useState } from 'react';
import { getTray, getTrays } from '../api/trays';
import { errorMessage } from '../api/request';
import type { Part } from '../types/part';
import type { Tray } from '../types/tray';
import type { Page } from '../types/pagination';
import { PART_TYPES } from '../types/partType';
import type { PartType } from '../types/partType';
import { formatDate, formatPartType, formatWeight } from '../utils/format';
import Pagination from './Pagination';
import PartsTable from './PartsTable';

interface InventoryBrowserProps {
    selectedParts: Part[];
    onTogglePart: (part: Part) => void;
    onReconcileTrays: (trays: Tray[]) => void;
    refreshKey: number;
    onRefresh: () => void;
    disabled?: boolean;
}

export default function InventoryBrowser({ selectedParts, onTogglePart, onReconcileTrays,
    refreshKey, onRefresh, disabled = false }: InventoryBrowserProps) {
    const [type, setType] = useState<PartType | ''>('');
    const [availableOnly, setAvailableOnly] = useState(true);
    const [page, setPage] = useState(0);
    const [result, setResult] = useState<Page<Tray> | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [trayId, setTrayId] = useState<number | null>(null);
    const [tray, setTray] = useState<Tray | null>(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState('');

    useEffect(() => {
        const controller = new AbortController();
        async function loadTrays() {
            setIsLoading(true);
            setError('');
            try {
                const response = await getTrays({ type: type || undefined, availableOnly, page }, controller.signal);
                if (controller.signal.aborted) return;
                setResult(response);
                onReconcileTrays(response.content);
                if (page > 0 && response.content.length === 0) {
                    setPage(Math.max(0, response.totalPages - 1));
                }
            } catch (err) {
                if (!controller.signal.aborted) setError(errorMessage(err));
            } finally {
                if (!controller.signal.aborted) setIsLoading(false);
            }
        }
        void loadTrays();
        return () => controller.abort();
    }, [type, availableOnly, page, refreshKey, onReconcileTrays]);

    useEffect(() => {
        if (trayId === null) return;
        const controller = new AbortController();
        async function loadTray() {
            setDetailLoading(true);
            setDetailError('');
            try {
                const response = await getTray(trayId!, controller.signal);
                if (controller.signal.aborted) return;
                setTray(response);
                onReconcileTrays([response]);
            } catch (err) {
                if (!controller.signal.aborted) setDetailError(errorMessage(err));
            } finally {
                if (!controller.signal.aborted) setDetailLoading(false);
            }
        }
        void loadTray();
        return () => controller.abort();
    }, [trayId, refreshKey, onReconcileTrays]);

    return (
        <div>
            <div className="filters">
                <div className="field">
                    <label htmlFor="inventory-type">Part type</label>
                    <select id="inventory-type" value={type} disabled={disabled}
                        onChange={event => { setType(event.target.value as PartType | ''); setPage(0); }}>
                        <option value="">All types</option>
                        {PART_TYPES.map(partType => <option key={partType} value={partType}>{formatPartType(partType)}</option>)}
                    </select>
                </div>
                <label className="checkbox-label">
                    <input type="checkbox" checked={availableOnly} disabled={disabled}
                        onChange={event => { setAvailableOnly(event.target.checked); setPage(0); }} />
                    Only trays with available parts
                </label>
                <button type="button" className="button-secondary" disabled={isLoading || disabled}
                    onClick={onRefresh}>Refresh inventory</button>
            </div>

            {error && <div className="form-message error" role="alert">{error}</div>}
            <div className="table-wrapper">
                <table>
                    <thead><tr><th>Tray ID</th><th>Part type</th><th>Total weight</th><th>Available weight</th>
                        <th>Available parts</th><th>Received at</th><th>Contents</th></tr></thead>
                    <tbody>
                    {isLoading || error ? (
                        <tr><td colSpan={7} className="empty-state">{isLoading ? 'Loading received trays...' : 'Inventory could not be loaded.'}</td></tr>
                    ) : !result?.content.length ? (
                        <tr><td colSpan={7} className="empty-state">
                            {availableOnly ? 'No available trays match these filters. Refresh after more sealed trays arrive.' : 'No received trays match these filters.'}
                        </td></tr>
                    ) : result.content.map(item => (
                        <tr key={item.id}>
                            <td>{item.id}</td><td>{formatPartType(item.type)}</td>
                            <td>{formatWeight(item.totalWeight)}</td><td>{formatWeight(item.availableWeight)}</td>
                            <td>{item.parts.filter(part => part.productId === null).length} / {item.parts.length}</td>
                            <td>{formatDate(item.receivedAt)}</td>
                            <td><button type="button" className="button-secondary" disabled={disabled}
                                aria-expanded={trayId === item.id} onClick={() => {
                                    setTrayId(trayId === item.id ? null : item.id); setTray(null); setDetailError('');
                                }}>{trayId === item.id ? 'Close parts' : 'View parts'}</button></td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
            {!error && result && <Pagination page={page} totalPages={result.totalPages}
                totalElements={result.totalElements} onPageChange={setPage} isLoading={isLoading || disabled} label="trays" />}

            {trayId !== null && (
                <section className="detail-panel" aria-label={`Tray ${trayId} contents`}>
                    <div className="detail-heading">
                        <h3>Tray {trayId}</h3>
                        <button type="button" className="button-secondary" disabled={disabled}
                            onClick={() => { setTrayId(null); setTray(null); }}>Close</button>
                    </div>
                    {detailLoading ? <p className="muted" role="status">Loading tray contents...</p>
                        : detailError ? <div className="form-message error" role="alert">{detailError}</div>
                        : tray?.id === trayId && <>
                            <div className="detail-grid">
                                <div className="detail-item"><span>Part type</span><strong>{formatPartType(tray.type)}</strong></div>
                                <div className="detail-item"><span>Total weight</span><strong>{formatWeight(tray.totalWeight)}</strong></div>
                                <div className="detail-item"><span>Available weight</span><strong>{formatWeight(tray.availableWeight)}</strong></div>
                                <div className="detail-item"><span>Sealed at</span><strong>{formatDate(tray.sealedAt)}</strong></div>
                            </div>
                            <p className="muted">Select available parts to include them in a product. Consumed parts cannot be selected.</p>
                            <PartsTable parts={tray.parts} selectedParts={selectedParts} onToggle={onTogglePart} disabled={disabled} />
                        </>}
                </section>
            )}
        </div>
    );
}
