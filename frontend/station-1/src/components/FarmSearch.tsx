
import { useEffect, useRef, useState } from 'react';
import { searchFarms } from '../api/farms';
import type { Farm } from '../types/farm';

interface FarmSearchProps {
    selectedFarm: Farm | null;
    onSelect: (farm: Farm | null) => void;
}

export default function FarmSearch({ selectedFarm, onSelect }: FarmSearchProps) {
    const [query, setQuery] = useState(selectedFarm?.name ?? '');
    const [results, setResults] = useState<Farm[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(false);

    const containerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!isOpen || !query.trim()) {
            return;
        }

        const controller = new AbortController();

        const timeout = window.setTimeout(async () => {
            setIsLoading(true);
            setError(false);

            try {
                const farms = await searchFarms(query.trim(), controller.signal);
                if (!controller.signal.aborted) {
                    setResults(farms);
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    console.error('Failed to search farms:', error);
                    setError(true);
                    setResults([]);
                }
            } finally {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            }
        }, 250);

        return () => {
            window.clearTimeout(timeout);
            controller.abort();
        };
    }, [query, isOpen]);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (
                containerRef.current &&
                !containerRef.current.contains(event.target as Node)
            ) {
                setIsOpen(false);
            }
        }

        document.addEventListener('mousedown', handleClickOutside);

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    function handleChange(value: string) {
        setQuery(value);
        setResults([]);
        setError(false);
        onSelect(null);
        setIsOpen(true);
    }

    function handleSelect(farm: Farm) {
        setQuery(farm.name);
        onSelect(farm);
        setIsOpen(false);
        setResults([]);
    }

    return (
        <div className="farm-search" ref={containerRef}>
            <label htmlFor="origin-farm">Origin farm</label>

            <input
                id="origin-farm"
                type="text"
                value={query}
                placeholder="Search for a farm..."
                autoComplete="off"
                required
                aria-expanded={isOpen}
                aria-controls="farm-suggestions"
                onChange={event => handleChange(event.target.value)}
                onFocus={() => {
                    if (!selectedFarm && query.trim()) {
                        setIsOpen(true);
                    }
                }}
                onKeyDown={event => {
                    if (event.key === 'Escape') {
                        setIsOpen(false);
                    }
                }}
            />

            {isOpen && (
                <div id="farm-suggestions" className="farm-suggestions">
                    {isLoading && (
                        <div className="search-message">Searching...</div>
                    )}

                    {!isLoading && error && (
                        <div className="search-message">
                            Unable to search farms.
                        </div>
                    )}

                    {!isLoading && !error && results.length === 0 && (
                        <div className="search-message">
                            No farms found.
                        </div>
                    )}

                    {!isLoading && !error && results.map(farm => (
                        <button
                            key={farm.id}
                            type="button"
                            className="farm-option"
                            onClick={() => handleSelect(farm)}
                        >
                            {farm.name}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
