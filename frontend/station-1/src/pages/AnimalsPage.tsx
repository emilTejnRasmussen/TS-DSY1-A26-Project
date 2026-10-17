
import { useEffect, useState } from 'react';
import { getAnimal, getAnimals } from '../api/animals';
import type { Animal } from '../types/animal';
import FarmSearch from '../components/FarmSearch';
import type { Farm } from '../types/farm';

const PAGE_SIZE = 10;

export default function AnimalsPage() {
    const [animals, setAnimals] = useState<Animal[]>([]);
    const [registrationNumber, setRegistrationNumber] = useState('');
    const [registeredDate, setRegisteredDate] = useState('');
    const [selectedFarm, setSelectedFarm] = useState<Farm | null>(null);

    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [sort, setSort] = useState('registeredAt,desc');

    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    const [lookupResult, setLookupResult] = useState<Animal | null>(null);
    const [lookupError, setLookupError] = useState('');
    const [isLookingUp, setIsLookingUp] = useState(false);

    useEffect(() => {
        const controller = new AbortController();

        async function loadAnimals() {
            setIsLoading(true);
            setError('');

            try {
                const result = await getAnimals({
                    registeredDate: registeredDate || undefined,
                    farmId: selectedFarm?.id,
                    page,
                    size: PAGE_SIZE,
                    sort,
                }, controller.signal);

                if (controller.signal.aborted) return;

                setAnimals(result.content);
                setTotalPages(result.totalPages);
                setTotalElements(result.totalElements);
            } catch (err) {
                if (controller.signal.aborted) return;

                console.error('Failed to load animals:', err);
                setError('Unable to load animals.');
            } finally {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            }
        }

        void loadAnimals();

        return () => controller.abort();
    }, [registeredDate, selectedFarm, page, sort]);

    async function handleLookup(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();

        const number = registrationNumber.trim();

        if (!number) return;

        setIsLookingUp(true);
        setLookupError('');
        setLookupResult(null);

        try {
            const animal = await getAnimal(number);
            setLookupResult(animal);
        } catch (err) {
            console.error('Animal lookup failed:', err);
            setLookupError('No animal found with that registration number.');
        } finally {
            setIsLookingUp(false);
        }
    }

    function resetFilters() {
        setRegisteredDate('');
        setSelectedFarm(null);
        setPage(0);
        setSort('registeredAt,desc');
    }

    function formatType(type: string) {
        return type.charAt(0) + type.slice(1).toLowerCase();
    }

    function formatDate(date: string) {
        return new Date(date).toLocaleString('en-GB', {
            dateStyle: 'short',
            timeStyle: 'short',
        });
    }

    return (
        <section>
            <div className="section-heading">
                <h2>Animals</h2>
                <p>Browse and find registered animals.</p>
            </div>

            <form className="animal-lookup" onSubmit={handleLookup}>
                <input
                    type="search"
                    className="search-input"
                    placeholder="Enter registration number..."
                    aria-label="Registration number"
                    value={registrationNumber}
                    onChange={event => setRegistrationNumber(event.target.value)}
                />

                <button
                    className="button-secondary"
                    type="submit"
                    disabled={isLookingUp || !registrationNumber.trim()}
                >
                    {isLookingUp ? 'Searching...' : 'Find animal'}
                </button>
            </form>

            {lookupError && (
                <div className="form-message error" role="alert">
                    {lookupError}
                </div>
            )}

            {lookupResult && (
                <div className="animal-lookup-result">
                    <strong>{lookupResult.registrationNumber}</strong>
                    <span>{formatType(lookupResult.type)}</span>
                    <span>{lookupResult.weight} kg</span>
                    <span>{lookupResult.farmName}</span>
                    <span>{formatDate(lookupResult.registeredAt)}</span>
                    <button
                        type="button"
                        className="button-secondary"
                        onClick={() => setLookupResult(null)}
                    >
                        Close
                    </button>
                </div>
            )}

            <div className="animal-filters">
                <div className="field">
                    <label htmlFor="registered-date">Registration date</label>
                    <input
                        id="registered-date"
                        type="date"
                        value={registeredDate}
                        onChange={event => {
                            setRegisteredDate(event.target.value);
                            setPage(0);
                        }}
                    />
                </div>

                <div className="field">
                    <FarmSearch
                        selectedFarm={selectedFarm}
                        onSelect={farm => {
                            setSelectedFarm(farm);
                            setPage(0);
                        }}
                    />
                </div>

                <div className="field">
                    <label htmlFor="animal-sort">Sort by</label>
                    <select
                        id="animal-sort"
                        value={sort}
                        onChange={event => {
                            setSort(event.target.value);
                            setPage(0);
                        }}
                    >
                        <option value="registeredAt,desc">Newest first</option>
                        <option value="registeredAt,asc">Oldest first</option>
                        <option value="weight,desc">Highest weight</option>
                        <option value="weight,asc">Lowest weight</option>
                    </select>
                </div>

                <button
                    type="button"
                    className="button-secondary reset-filters"
                    onClick={resetFilters}
                >
                    Clear filters
                </button>
            </div>

            <div className="table-wrapper">
                <table className="animals-table">
                    <colgroup>
                        <col style={{ width: '40%' }} />
                        <col style={{ width: '13%' }} />
                        <col style={{ width: '15%' }} />
                        <col style={{ width: '16%' }} />
                        <col style={{ width: '16%' }} />
                    </colgroup>

                    <thead>
                    <tr>
                        <th>Registration number</th>
                        <th>Type</th>
                        <th>Weight</th>
                        <th>Farm</th>
                        <th>Registered at</th>
                    </tr>
                    </thead>

                    <tbody>
                    {isLoading || error ? (
                        <tr>
                            <td colSpan={5} className="empty-state">
                                {isLoading ? 'Loading animals...' : error}
                            </td>
                        </tr>
                    ) : (
                        <>
                            {animals.length === 0 && (
                                <tr>
                                    <td colSpan={5} className="empty-state">
                                        No animals found.
                                    </td>
                                </tr>
                            )}

                            {animals.map(animal => (
                                <tr key={animal.registrationNumber}>
                                    <td title={animal.registrationNumber}>
                                        {animal.registrationNumber}
                                    </td>
                                    <td>{formatType(animal.type)}</td>
                                    <td>{animal.weight} kg</td>
                                    <td title={animal.farmName}>
                                        {animal.farmName}
                                    </td>
                                    <td>{formatDate(animal.registeredAt)}</td>
                                </tr>
                            ))}

                            {Array.from({
                                length: PAGE_SIZE - Math.max(animals.length, 1)
                            }, (_, index) => (
                                <tr
                                    key={`empty-${index}`}
                                    className="table-empty-row"
                                    aria-hidden="true"
                                >
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                </tr>
                            ))}
                        </>
                    )}
                    </tbody>
                </table>
            </div>

            {!isLoading && !error && totalPages > 0 && (
                <div className="pagination">
                    <span className="pagination-info">
                        {totalElements} animals · Page {page + 1} of {totalPages}
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
