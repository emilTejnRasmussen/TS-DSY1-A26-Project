
import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { getRecentAnimals, registerAnimal } from '../api/animals';
import FarmSearch from '../components/FarmSearch';
import { ANIMAL_TYPES } from '../types/animal';
import type { Animal, AnimalType } from '../types/animal';
import type { Farm } from '../types/farm';

export default function RegistrationPage() {
    const [animalType, setAnimalType] = useState<AnimalType>('PIG');
    const [selectedFarm, setSelectedFarm] = useState<Farm | null>(null);
    const [weight, setWeight] = useState('');

    const [recentAnimals, setRecentAnimals] = useState<Animal[]>([]);
    const [isLoadingRecent, setIsLoadingRecent] = useState(true);
    const [recentError, setRecentError] = useState('');

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [registeredAnimal, setRegisteredAnimal] = useState<Animal | null>(null);
    const [error, setError] = useState('');

    const weightInputRef = useRef<HTMLInputElement>(null);
    const recentRequestId = useRef(0);
    const mounted = useRef(true);

    useEffect(() => {
        mounted.current = true;
        void loadRecentAnimals();

        return () => {
            mounted.current = false;
            recentRequestId.current++;
        };
    }, []);

    async function loadRecentAnimals(): Promise<void> {
        const requestId = ++recentRequestId.current;

        setIsLoadingRecent(true);
        setRecentError('');

        try {
            const animals = await getRecentAnimals();

            if (!mounted.current || requestId !== recentRequestId.current) {
                return;
            }

            setRecentAnimals(animals);
        } catch (error) {
            if (!mounted.current || requestId !== recentRequestId.current) {
                return;
            }

            console.error('Failed to load recent animals:', error);
            setRecentError('Unable to load recent registrations.');
        } finally {
            if (mounted.current && requestId === recentRequestId.current) {
                setIsLoadingRecent(false);
            }
        }
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
        event.preventDefault();

        if (isSubmitting) return;

        setError('');
        setRegisteredAnimal(null);

        if (!selectedFarm) {
            setError('Please select an existing farm from the search results.');
            return;
        }

        const parsedWeight = Number(weight);

        if (!Number.isFinite(parsedWeight) || parsedWeight <= 0) {
            setError('Please enter a valid weight greater than zero.');
            return;
        }

        setIsSubmitting(true);

        try {
            const animal = await registerAnimal({
                type: animalType,
                weight: parsedWeight,
                farmId: selectedFarm.id
            });

            if (!mounted.current) return;

            setRegisteredAnimal(animal);
            setWeight('');

            await loadRecentAnimals();

            weightInputRef.current?.focus();
        } catch (error) {
            if (!mounted.current) return;

            console.error('Animal registration failed:', error);
            setError('Unable to register animal. Please try again.');
        } finally {
            if (mounted.current) {
                setIsSubmitting(false);
            }
        }
    }

    return (
        <section>
            <div className="section-heading">
                <h2>Register animal</h2>
                <p>Enter the details of the arriving animal.</p>
            </div>

            <form className="registration-form" onSubmit={handleSubmit}>
                <div className="form-grid">
                    <div className="field">
                        <label htmlFor="animal-type">Animal type</label>

                        <select
                            id="animal-type"
                            value={animalType}
                            onChange={event => {
                                setAnimalType(event.target.value as AnimalType);
                            }}
                        >
                            {ANIMAL_TYPES.map(type => (
                                <option key={type} value={type}>
                                    {type.charAt(0) + type.slice(1).toLowerCase()}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="field">
                        <FarmSearch
                            selectedFarm={selectedFarm}
                            onSelect={setSelectedFarm}
                        />
                    </div>
                </div>

                <div className="field">
                    <label htmlFor="animal-weight">Weight (kg)</label>

                    <input
                        ref={weightInputRef}
                        id="animal-weight"
                        type="number"
                        min="0.01"
                        step="0.01"
                        placeholder="Enter weight"
                        value={weight}
                        onChange={event => setWeight(event.target.value)}
                        required
                    />
                </div>

                {error && (
                    <div className="form-message error" role="alert">
                        {error}
                    </div>
                )}

                {registeredAnimal && (
                    <div className="form-message success" role="status">
                        <strong>Animal registered successfully.</strong>
                        <span>
                            Registration number: {registeredAnimal.registrationNumber}
                        </span>
                    </div>
                )}

                <div className="form-actions">
                    <button
                        className="button-primary"
                        type="submit"
                        disabled={isSubmitting}
                    >
                        {isSubmitting ? 'Registering...' : 'Register animal'}
                    </button>
                </div>
            </form>

            <section className="recent-section">
                <div className="section-heading">
                    <h2>Recent registrations</h2>
                    <p>The five most recently registered animals.</p>
                </div>

                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>Registration number</th>
                            <th>Type</th>
                            <th>Weight</th>
                            <th>Farm</th>
                        </tr>
                        </thead>

                        <tbody>
                        {isLoadingRecent ? (
                            <tr>
                                <td colSpan={4} className="empty-state">
                                    Loading registrations...
                                </td>
                            </tr>
                        ) : recentError ? (
                            <tr>
                                <td colSpan={4} className="empty-state">
                                    {recentError}
                                </td>
                            </tr>
                        ) : recentAnimals.length === 0 ? (
                            <tr>
                                <td colSpan={4} className="empty-state">
                                    No registrations to display.
                                </td>
                            </tr>
                        ) : (
                            recentAnimals.map(animal => (
                                <tr key={animal.registrationNumber}>
                                    <td>{animal.registrationNumber}</td>
                                    <td>
                                        {animal.type.charAt(0) +
                                            animal.type.slice(1).toLowerCase()}
                                    </td>
                                    <td>{animal.weight} kg</td>
                                    <td>{animal.farmName}</td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>
            </section>
        </section>
    );
}
