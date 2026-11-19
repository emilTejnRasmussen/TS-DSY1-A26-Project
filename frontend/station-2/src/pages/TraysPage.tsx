import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { createTray, getTrays } from '../api/trays';
import { errorMessage } from '../api/request';
import Pagination from '../components/Pagination';
import TrayDetail from '../components/TrayDetail';
import { PART_TYPES } from '../types/partType';
import type { PartType } from '../types/partType';
import type { Tray, TrayStatus } from '../types/tray';
import { formatDate, formatPartType, formatWeight } from '../utils/format';

export default function TraysPage() {
    const [type, setType] = useState<PartType>('LEG');
    const [maxWeight, setMaxWeight] = useState('');
    const [isCreating, setIsCreating] = useState(false);
    const [createError, setCreateError] = useState('');
    const [success, setSuccess] = useState('');
    const [trays, setTrays] = useState<Tray[]>([]);
    const [filterType, setFilterType] = useState<PartType | ''>('');
    const [filterStatus, setFilterStatus] = useState<TrayStatus | ''>('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);
    const mounted = useRef(false);

    useEffect(() => {
        mounted.current = true;
        return () => { mounted.current = false; };
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        async function load() {
            setIsLoading(true);
            setLoadError('');
            try {
                const result = await getTrays({ type: filterType || undefined, status: filterStatus || undefined,
                    page, size: 10 }, controller.signal);
                if (controller.signal.aborted) return;
                setTrays(result.content);
                setTotalPages(result.totalPages);
                setTotalElements(result.totalElements);
            } catch (error) {
                if (!controller.signal.aborted) setLoadError(errorMessage(error));
            } finally {
                if (!controller.signal.aborted) setIsLoading(false);
            }
        }
        void load();
        return () => controller.abort();
    }, [filterType, filterStatus, page, refreshKey]);

    async function handleCreate(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (isCreating) return;
        setCreateError('');
        setSuccess('');
        const parsedWeight = Number(maxWeight);
        if (!Number.isFinite(parsedWeight) || parsedWeight < 0.01) {
            setCreateError('Enter a maximum weight of at least 0.01 kg.');
            return;
        }
        setIsCreating(true);
        try {
            const tray = await createTray({ type, maxWeight: parsedWeight });
            if (!mounted.current) return;
            setSuccess(`Tray ${tray.id} created successfully.`);
            setMaxWeight('');
            setSelectedId(tray.id);
            setPage(0);
            setRefreshKey(key => key + 1);
        } catch (error) {
            if (mounted.current) setCreateError(errorMessage(error));
        } finally {
            if (mounted.current) setIsCreating(false);
        }
    }

    return (
        <section>
            <div className="section-heading"><h2>Create tray</h2><p>Set the part type and maximum weight for an empty tray.</p></div>
            <form className="registration-form" onSubmit={handleCreate}>
                <fieldset disabled={isCreating}>
                    <div className="form-grid">
                        <div className="field"><label htmlFor="tray-type">Part type</label>
                            <select id="tray-type" value={type} onChange={event => setType(event.target.value as PartType)}>
                                {PART_TYPES.map(value => <option key={value} value={value}>{formatPartType(value)}</option>)}
                            </select></div>
                        <div className="field"><label htmlFor="tray-capacity">Maximum weight (kg)</label>
                            <input id="tray-capacity" type="number" min="0.01" max="99999999.99" step="0.01"
                                value={maxWeight} onChange={event => setMaxWeight(event.target.value)} required /></div>
                    </div>
                    {createError && <div className="form-message error" role="alert">{createError}</div>}
                    {success && <div className="form-message success" role="status">{success}</div>}
                    <div className="form-actions"><button className="button-primary" type="submit">{isCreating ? 'Creating...' : 'Create tray'}</button></div>
                </fieldset>
            </form>

            <section className="section-divider">
                <div className="detail-heading">
                    <div className="section-heading"><h2>Trays</h2><p>Inspect tray contents, assign parts, and seal populated trays.</p></div>
                    <button type="button" className="button-secondary" disabled={isLoading}
                        onClick={() => setRefreshKey(key => key + 1)}>Refresh</button>
                </div>
                <div className="filters">
                    <div className="field"><label htmlFor="tray-type-filter">Part type</label>
                        <select id="tray-type-filter" value={filterType} onChange={event => {
                            setFilterType(event.target.value as PartType | ''); setPage(0);
                        }}><option value="">All types</option>
                            {PART_TYPES.map(value => <option key={value} value={value}>{formatPartType(value)}</option>)}
                        </select></div>
                    <div className="field"><label htmlFor="tray-status-filter">Status</label>
                        <select id="tray-status-filter" value={filterStatus} onChange={event => {
                            setFilterStatus(event.target.value as TrayStatus | ''); setPage(0);
                        }}><option value="">All statuses</option><option value="OPEN">Open</option><option value="SEALED">Sealed</option></select></div>
                    <button className="button-secondary" type="button" onClick={() => {
                        setFilterType(''); setFilterStatus(''); setPage(0);
                    }}>Clear filters</button>
                </div>
                {loadError && <div className="form-message error" role="alert">{loadError}</div>}
                <div className="table-wrapper"><table>
                    <thead><tr><th>Tray ID</th><th>Type</th><th>Weight / maximum</th><th>Remaining capacity</th><th>Status</th><th>Created at</th><th>Action</th></tr></thead>
                    <tbody>{isLoading || loadError || trays.length === 0 ? <tr><td colSpan={7} className="empty-state">
                        {isLoading ? 'Loading trays...' : loadError ? 'Trays could not be loaded. Try Refresh.' : 'No trays found.'}
                    </td></tr> : trays.map(tray => <tr key={tray.id}>
                        <td>{tray.id}</td><td>{formatPartType(tray.type)}</td>
                        <td>{formatWeight(tray.currentWeight)} / {formatWeight(tray.maxWeight)}</td>
                        <td>{formatWeight(Math.max(0, tray.maxWeight - tray.currentWeight))}</td>
                        <td><span className={`status-badge status-${tray.status.toLowerCase()}`}>{tray.status === 'OPEN' ? 'Open' : 'Sealed'}</span></td>
                        <td>{formatDate(tray.createdAt)}</td>
                        <td><button className="button-secondary" type="button" aria-expanded={selectedId === tray.id}
                            onClick={() => setSelectedId(tray.id)}>View tray</button></td>
                    </tr>)}</tbody>
                </table></div>
                {!loadError && <Pagination page={page} totalPages={totalPages} totalElements={totalElements}
                    onPageChange={setPage} isLoading={isLoading} label="trays" />}
                {selectedId !== null && <TrayDetail key={selectedId} trayId={selectedId}
                    onClose={() => setSelectedId(null)} onChanged={() => setRefreshKey(key => key + 1)} />}
            </section>
        </section>
    );
}
