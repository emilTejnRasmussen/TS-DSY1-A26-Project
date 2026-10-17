
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createFarm, getFarms } from '../api/farms';
import type { Farm } from '../types/farm';

const PAGE_SIZE = 10;

export default function FarmsPage() {
    const [farms, setFarms] = useState<Farm[]>([]);
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');

    const [showCreateForm, setShowCreateForm] = useState(false);
    const [farmName, setFarmName] = useState('');
    const [isCreating, setIsCreating] = useState(false);
    const [createError, setCreateError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const [refreshKey, setRefreshKey] = useState(0);

    useEffect(() => {
        const timeout = window.setTimeout(() => {
            setDebouncedSearch(search.trim());
            setPage(0);
        }, 250);

        return () => window.clearTimeout(timeout);
    }, [search]);

    useEffect(() => {
        const controller = new AbortController();

        async function loadFarms() {
            setIsLoading(true);
            setLoadError('');

            try {
                const result = await getFarms(
                    debouncedSearch,
                    page,
                    PAGE_SIZE,
                    controller.signal
                );

                if (controller.signal.aborted) return;

                setFarms(result.content);
                setTotalPages(result.totalPages);
                setTotalElements(result.totalElements);
            } catch (error) {
                if (controller.signal.aborted) return;

                console.error('Failed to load farms:', error);
                setLoadError('Unable to load farms. Please try again.');
            } finally {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            }
        }

        void loadFarms();

        return () => controller.abort();
    }, [debouncedSearch, page, refreshKey]);

    async function handleCreateFarm(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (isCreating) return;

        const name = farmName.trim();

        if (!name) {
            setCreateError('Please enter a farm name.');
            return;
        }

        setIsCreating(true);
        setCreateError('');
        setSuccessMessage('');

        try {
            const farm = await createFarm(name);

            setSuccessMessage(`Farm "${farm.name}" created successfully.`);
            setFarmName('');
            setShowCreateForm(false);

            setSearch('');
            setDebouncedSearch('');
            setPage(0);
            setRefreshKey(key => key + 1);
        } catch (error) {
            console.error('Failed to create farm:', error);
            setCreateError('Unable to create farm. Please try again.');
        } finally {
            setIsCreating(false);
        }
    }

    return (
        <section>
            <div className="section-heading">
                <h2>Farms</h2>
                <p>Browse and register originating farms.</p>
            </div>

            <div className="farms-toolbar">
                <input
                    type="search"
                    className="search-input"
                    placeholder="Search farms..."
                    aria-label="Search farms"
                    value={search}
                    onChange={event => setSearch(event.target.value)}
                />

                <button
                    type="button"
                    className="button-primary"
                    onClick={() => {
                        setShowCreateForm(current => !current);
                        setCreateError('');
                        setSuccessMessage('');
                    }}
                >
                    {showCreateForm ? 'Cancel' : '+ New farm'}
                </button>
            </div>

            {showCreateForm && (
                <form className="create-farm-form" onSubmit={handleCreateFarm}>
                    <h3>Register new farm</h3>

                    <div className="field">
                        <label htmlFor="farm-name">Farm name</label>
                        <input
                            id="farm-name"
                            type="text"
                            placeholder="Enter farm name"
                            value={farmName}
                            onChange={event => setFarmName(event.target.value)}
                            maxLength={150}
                            required
                        />
                    </div>

                    {createError && (
                        <div className="form-message error" role="alert">
                            {createError}
                        </div>
                    )}

                    <div className="form-actions">
                        <button
                            className="button-primary"
                            type="submit"
                            disabled={isCreating || !farmName.trim()}
                        >
                            {isCreating ? 'Creating...' : 'Create farm'}
                        </button>
                    </div>
                </form>
            )}

            {successMessage && (
                <div className="form-message success" role="status">
                    {successMessage}
                </div>
            )}

            <div className="table-wrapper">
                <table className="farms-table">
                    <colgroup>
                        <col style={{ width: '75%' }} />
                        <col style={{ width: '25%' }} />
                    </colgroup>

                    <thead>
                    <tr>
                        <th>Farm name</th>
                        <th>ID</th>
                    </tr>
                    </thead>

                    <tbody>
                    {isLoading ? (
                        <tr>
                            <td colSpan={2} className="empty-state">
                                Loading farms...
                            </td>
                        </tr>
                    ) : loadError ? (
                        <tr>
                            <td colSpan={2} className="empty-state">
                                {loadError}
                            </td>
                        </tr>
                    ) : farms.length === 0 ? (
                        <tr>
                            <td colSpan={2} className="empty-state">
                                No farms found.
                            </td>
                        </tr>
                    ) : (
                        <>
                            {farms.map(farm => (
                                <tr key={farm.id}>
                                    <td>{farm.name}</td>
                                    <td>{farm.id}</td>
                                </tr>
                            ))}

                            {Array.from(
                                { length: PAGE_SIZE - farms.length },
                                (_, index) => (
                                    <tr key={`empty-${index}`} className="table-empty-row" aria-hidden="true">
                                        <td>&nbsp;</td>
                                        <td>&nbsp;</td>
                                    </tr>
                                )
                            )}
                        </>
                    )}
                    </tbody>
                </table>
            </div>

            {!isLoading && !loadError && totalPages > 0 && (
                <div className="pagination">
                    <span className="pagination-info">
                        {totalElements} farms · Page {page + 1} of {totalPages}
                    </span>

                    <div className="pagination-actions">
                        <button
                            type="button"
                            className="button-secondary"
                            disabled={page === 0}
                            onClick={() => setPage(current => current - 1)}
                        >
                            Previous
                        </button>

                        <button
                            type="button"
                            className="button-secondary"
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage(current => current + 1)}
                        >
                            Next
                        </button>
                    </div>
                </div>
            )}
        </section>
    );
}
