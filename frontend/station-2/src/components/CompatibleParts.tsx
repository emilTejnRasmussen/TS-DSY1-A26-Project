import { useEffect, useState } from 'react';
import { getParts } from '../api/parts';
import { errorMessage } from '../api/request';
import type { Part } from '../types/part';
import type { PartType } from '../types/partType';
import { formatPartType, formatWeight } from '../utils/format';
import Pagination from './Pagination';

interface CompatiblePartsProps {
    type: PartType;
    refreshKey: number;
    isBusy: boolean;
    onAdd: (partId: number) => void;
}

export default function CompatibleParts({ type, refreshKey, isBusy, onAdd }: CompatiblePartsProps) {
    const [parts, setParts] = useState<Part[]>([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const controller = new AbortController();
        async function load() {
            setIsLoading(true);
            setError('');
            try {
                const result = await getParts({ partType: type, page, size: 10 }, controller.signal);
                if (controller.signal.aborted) return;
                setParts(result.content);
                setTotalPages(result.totalPages);
                setTotalElements(result.totalElements);
            } catch (error) {
                if (!controller.signal.aborted) setError(errorMessage(error));
            } finally {
                if (!controller.signal.aborted) setIsLoading(false);
            }
        }
        void load();
        return () => controller.abort();
    }, [type, page, refreshKey]);

    return (
        <section className="section-divider">
            <div className="section-heading">
                <h3>Add {formatPartType(type).toLowerCase()} parts</h3>
                <p>Choose an unassigned part. Assigned parts are shown for reference and cannot be added.</p>
            </div>
            {error && <div className="form-message error" role="alert">{error}</div>}
            <div className="table-wrapper">
                <table><thead><tr><th>Part ID</th><th>Animal registration number</th><th>Weight</th><th>Assignment</th><th>Action</th></tr></thead>
                    <tbody>
                        {isLoading || error || parts.length === 0 ? <tr><td colSpan={5} className="empty-state">
                            {isLoading ? 'Loading compatible parts...' : error ? 'Parts could not be loaded.' : 'No matching parts found.'}
                        </td></tr> : parts.map(part => <tr key={part.id}>
                            <td>{part.id}</td><td>{part.animalRegistrationNumber}</td><td>{formatWeight(part.weight)}</td>
                            <td>{part.trayId === null ? <span className="status-badge status-available">Unassigned</span> : `Tray ${part.trayId}`}</td>
                            <td><button className="button-secondary" type="button" disabled={isBusy || part.trayId !== null}
                                onClick={() => onAdd(part.id)}>Add part</button></td>
                        </tr>)}
                    </tbody></table>
            </div>
            {!error && <Pagination page={page} totalPages={totalPages} totalElements={totalElements}
                onPageChange={setPage} isLoading={isLoading || isBusy} label="matching parts" />}
        </section>
    );
}
