import { useEffect, useRef, useState } from 'react';
import { getPart } from '../api/parts';
import { addPart, getTray, removePart, sealTray } from '../api/trays';
import { errorMessage } from '../api/request';
import type { Part } from '../types/part';
import type { Tray } from '../types/tray';
import { formatDate, formatPartType, formatWeight } from '../utils/format';
import CompatibleParts from './CompatibleParts';

interface TrayDetailProps {
    trayId: number;
    onClose: () => void;
    onChanged: () => void;
}

export default function TrayDetail({ trayId, onClose, onChanged }: TrayDetailProps) {
    const [tray, setTray] = useState<Tray | null>(null);
    const [parts, setParts] = useState<Part[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [actionError, setActionError] = useState('');
    const [busy, setBusy] = useState('');
    const [success, setSuccess] = useState('');
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
                const current = await getTray(trayId, controller.signal);
                const contents = await Promise.all(current.partIds.map(id => getPart(id, controller.signal)));
                if (controller.signal.aborted) return;
                setTray(current);
                setParts(contents);
            } catch (error) {
                if (!controller.signal.aborted) setLoadError(errorMessage(error));
            } finally {
                if (!controller.signal.aborted) setIsLoading(false);
            }
        }
        void load();
        return () => controller.abort();
    }, [trayId, refreshKey]);

    async function changeTray(operation: () => Promise<Tray>, label: string) {
        if (busy || isLoading) return;
        setBusy(label);
        setActionError('');
        setSuccess('');
        try {
            const result = await operation();
            if (!mounted.current) return;
            setTray(result);
            setSuccess(result.status === 'SEALED' ? `Tray ${result.id} sealed successfully.` : `Tray ${result.id} updated.`);
            onChanged();
        } catch (error) {
            if (!mounted.current) return;
            setActionError(errorMessage(error));
            onChanged();
        } finally {
            if (mounted.current) {
                setBusy('');
                setRefreshKey(key => key + 1);
            }
        }
    }

    return (
        <section className="detail-panel" aria-label={`Tray ${trayId} details`}>
            <div className="detail-heading">
                <h3>Tray {trayId}</h3>
                <div className="actions">
                    <button className="button-secondary" type="button" disabled={!!busy || isLoading}
                        onClick={() => setRefreshKey(key => key + 1)}>Refresh tray</button>
                    <button className="button-secondary" type="button" disabled={!!busy} onClick={onClose}>Close</button>
                </div>
            </div>
            {actionError && <div className="form-message error" role="alert">{actionError}</div>}
            {success && <div className="form-message success" role="status">{success}</div>}
            {loadError && <div className="form-message error" role="alert">{loadError}</div>}
            {isLoading ? <p className="muted" role="status">Loading tray contents...</p> : tray && !loadError && (
                <>
                    <div className="detail-grid">
                        <div className="detail-item"><span>Part type</span><strong>{formatPartType(tray.type)}</strong></div>
                        <div className="detail-item"><span>Status</span><strong className={`status-badge status-${tray.status.toLowerCase()}`}>{tray.status === 'OPEN' ? 'Open' : 'Sealed'}</strong></div>
                        <div className="detail-item"><span>Current weight</span><strong>{formatWeight(tray.currentWeight)}</strong></div>
                        <div className="detail-item"><span>Maximum weight</span><strong>{formatWeight(tray.maxWeight)}</strong></div>
                        <div className="detail-item"><span>Remaining capacity</span><strong>{formatWeight(Math.max(0, tray.maxWeight - tray.currentWeight))}</strong></div>
                        <div className="detail-item"><span>Created at</span><strong>{formatDate(tray.createdAt)}</strong></div>
                        {tray.sealedAt && <div className="detail-item"><span>Sealed at</span><strong>{formatDate(tray.sealedAt)}</strong></div>}
                    </div>
                    <div className="capacity-label"><span>Tray capacity</span><span>{formatWeight(tray.currentWeight)} / {formatWeight(tray.maxWeight)}</span></div>
                    <progress className="capacity-bar" value={tray.currentWeight} max={tray.maxWeight}
                        aria-label={`Tray ${tray.id} capacity`} />
                    <div className="table-wrapper">
                        <table><thead><tr><th>Part ID</th><th>Animal registration number</th><th>Type</th><th>Weight</th>{tray.status === 'OPEN' && <th>Action</th>}</tr></thead>
                            <tbody>{parts.length === 0 ? <tr><td colSpan={tray.status === 'OPEN' ? 5 : 4} className="empty-state">This tray is empty.</td></tr> :
                                parts.map(part => <tr key={part.id}><td>{part.id}</td><td>{part.animalRegistrationNumber}</td>
                                    <td>{formatPartType(part.type)}</td><td>{formatWeight(part.weight)}</td>
                                    {tray.status === 'OPEN' && <td><button className="button-secondary" type="button" disabled={!!busy}
                                        onClick={() => void changeTray(() => removePart(tray.id, part.id), 'Removing part...')}>Remove</button></td>}
                                </tr>)}</tbody></table>
                    </div>
                    {tray.status === 'SEALED' ? <p className="muted">This tray is sealed and cannot be modified.</p> : (
                        <>
                            <div className="toolbar">
                                <span className="small-text muted">Sealing is permanent. Add all required parts first.</span>
                                <button className="button-primary" type="button" disabled={!!busy || parts.length === 0}
                                    onClick={() => {
                                        if (window.confirm(`Seal tray ${tray.id}? Its contents cannot be changed after sealing.`)) {
                                            void changeTray(() => sealTray(tray.id), 'Sealing tray...');
                                        }
                                    }}>{busy || 'Seal tray'}</button>
                            </div>
                            <CompatibleParts type={tray.type} refreshKey={refreshKey} isBusy={!!busy}
                                onAdd={partId => void changeTray(() => addPart(tray.id, partId), 'Adding part...')} />
                        </>
                    )}
                </>
            )}
        </section>
    );
}
