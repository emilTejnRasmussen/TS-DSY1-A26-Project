import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { getParts, registerPart } from '../api/parts';
import { errorMessage } from '../api/request';
import Pagination from '../components/Pagination';
import type { Part } from '../types/part';
import { PART_TYPES } from '../types/partType';
import type { PartType } from '../types/partType';
import { formatDate, formatPartType, formatWeight } from '../utils/format';

export default function PartsPage() {
    const [animal, setAnimal] = useState('');
    const [type, setType] = useState<PartType>('LEG');
    const [weight, setWeight] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState('');
    const [success, setSuccess] = useState('');
    const [parts, setParts] = useState<Part[]>([]);
    const [filterAnimal, setFilterAnimal] = useState('');
    const [filterType, setFilterType] = useState<PartType | ''>('');
    const [sort, setSort] = useState('registeredAt,desc');
    const [filters, setFilters] = useState({ animal: '', type: '' as PartType | '', sort: 'registeredAt,desc' });
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
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
                const result = await getParts({
                    animalRegistrationNumber: filters.animal || undefined,
                    partType: filters.type || undefined, page, size: 10, sort: filters.sort,
                }, controller.signal);
                if (controller.signal.aborted) return;
                setParts(result.content);
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
    }, [filters, page, refreshKey]);

    async function handleRegister(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (isSubmitting) return;
        setSubmitError('');
        setSuccess('');
        const parsedWeight = Number(weight);
        if (!animal.trim() || !Number.isFinite(parsedWeight) || parsedWeight < 0.01) {
            setSubmitError('Enter an animal registration number and a weight of at least 0.01 kg.');
            return;
        }
        setIsSubmitting(true);
        try {
            const part = await registerPart({ animalRegistrationNumber: animal.trim(), type, weight: parsedWeight });
            if (!mounted.current) return;
            setSuccess(`Part ${part.id} registered for animal ${part.animalRegistrationNumber}.`);
            setWeight('');
            setPage(0);
            setRefreshKey(key => key + 1);
        } catch (error) {
            if (mounted.current) setSubmitError(errorMessage(error));
        } finally {
            if (mounted.current) setIsSubmitting(false);
        }
    }

    function applyFilters(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setFilters({ animal: filterAnimal.trim(), type: filterType, sort });
        setPage(0);
    }

    return (
        <section>
            <div className="section-heading">
                <h2>Register part</h2>
                <p>Register a weighed part against its originating animal.</p>
            </div>
            <form className="registration-form" onSubmit={handleRegister}>
                <fieldset disabled={isSubmitting}>
                    <div className="form-grid">
                        <div className="field">
                            <label htmlFor="part-animal">Animal registration number</label>
                            <input id="part-animal" value={animal} onChange={event => setAnimal(event.target.value)} required />
                        </div>
                        <div className="field">
                            <label htmlFor="part-type">Part type</label>
                            <select id="part-type" value={type} onChange={event => setType(event.target.value as PartType)}>
                                {PART_TYPES.map(value => <option key={value} value={value}>{formatPartType(value)}</option>)}
                            </select>
                        </div>
                        <div className="field">
                            <label htmlFor="part-weight">Weight (kg)</label>
                            <input id="part-weight" type="number" min="0.01" step="0.01" value={weight}
                                onChange={event => setWeight(event.target.value)} required />
                        </div>
                    </div>
                    {submitError && <div className="form-message error" role="alert">{submitError}</div>}
                    {success && <div className="form-message success" role="status">{success}</div>}
                    <div className="form-actions">
                        <button className="button-primary" type="submit">{isSubmitting ? 'Registering...' : 'Register part'}</button>
                    </div>
                </fieldset>
            </form>

            <section className="section-divider">
                <div className="detail-heading">
                    <div className="section-heading"><h2>Registered parts</h2><p>Find parts and see their tray assignments.</p></div>
                    <button className="button-secondary" type="button" disabled={isLoading}
                        onClick={() => setRefreshKey(key => key + 1)}>Refresh</button>
                </div>
                <form className="filters" onSubmit={applyFilters}>
                    <div className="field"><label htmlFor="filter-animal">Animal registration number</label>
                        <input id="filter-animal" value={filterAnimal} onChange={event => setFilterAnimal(event.target.value)} /></div>
                    <div className="field"><label htmlFor="filter-part-type">Part type</label>
                        <select id="filter-part-type" value={filterType} onChange={event => setFilterType(event.target.value as PartType | '')}>
                            <option value="">All types</option>
                            {PART_TYPES.map(value => <option key={value} value={value}>{formatPartType(value)}</option>)}
                        </select></div>
                    <div className="field"><label htmlFor="part-sort">Sort by</label>
                        <select id="part-sort" value={sort} onChange={event => setSort(event.target.value)}>
                            <option value="registeredAt,desc">Newest first</option><option value="registeredAt,asc">Oldest first</option>
                            <option value="weight,desc">Highest weight</option><option value="weight,asc">Lowest weight</option>
                        </select></div>
                    <div className="actions">
                        <button className="button-secondary" type="submit">Apply filters</button>
                        <button className="button-secondary" type="button" onClick={() => {
                            setFilterAnimal(''); setFilterType(''); setSort('registeredAt,desc');
                            setFilters({ animal: '', type: '', sort: 'registeredAt,desc' }); setPage(0);
                        }}>Clear</button>
                    </div>
                </form>
                {loadError && <div className="form-message error" role="alert">{loadError}</div>}
                <div className="table-wrapper">
                    <table><thead><tr><th>Part ID</th><th>Animal registration number</th><th>Type</th><th>Weight</th><th>Tray</th><th>Registered at</th></tr></thead>
                        <tbody>
                            {isLoading || loadError || parts.length === 0 ? <tr><td colSpan={6} className="empty-state">
                                {isLoading ? 'Loading parts...' : loadError ? 'Parts could not be loaded. Try Refresh.' : 'No parts found.'}
                            </td></tr> : parts.map(part => <tr key={part.id}>
                                <td>{part.id}</td><td>{part.animalRegistrationNumber}</td><td>{formatPartType(part.type)}</td>
                                <td>{formatWeight(part.weight)}</td><td>{part.trayId === null ?
                                    <span className="status-badge status-available">Unassigned</span> : `Tray ${part.trayId}`}</td>
                                <td>{formatDate(part.registeredAt)}</td>
                            </tr>)}
                        </tbody></table>
                </div>
                <p className="small-text muted">Registration times are currently unavailable.</p>
                {!loadError && <Pagination page={page} totalPages={totalPages} totalElements={totalElements}
                    onPageChange={setPage} isLoading={isLoading} label="parts" />}
            </section>
        </section>
    );
}
