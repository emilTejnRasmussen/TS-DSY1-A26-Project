import type { Part } from '../types/part';
import { formatPartType, formatWeight } from '../utils/format';

interface PartsTableProps {
    parts: Part[];
    selectedParts?: Part[];
    onToggle?: (part: Part) => void;
    disabled?: boolean;
}

export default function PartsTable({ parts, selectedParts = [], onToggle, disabled = false }: PartsTableProps) {
    return (
        <div className="table-wrapper">
            <table>
                <thead><tr>
                    {onToggle && <th>Select</th>}
                    <th>Part ID</th><th>Animal registration</th><th>Part type</th>
                    <th>Weight</th><th>Source tray</th><th>Availability</th>
                </tr></thead>
                <tbody>
                {parts.length === 0 ? (
                    <tr><td colSpan={onToggle ? 7 : 6} className="empty-state">No parts to display.</td></tr>
                ) : parts.map(part => (
                    <tr key={part.id}>
                        {onToggle && <td>
                            <input type="checkbox" aria-label={`Select part ${part.id}`}
                                disabled={disabled || part.productId !== null}
                                checked={part.productId === null && selectedParts.some(selected => selected.id === part.id)}
                                onChange={() => onToggle(part)} />
                        </td>}
                        <td>{part.id}</td><td>{part.animalRegistrationNumber}</td>
                        <td>{formatPartType(part.type)}</td><td>{formatWeight(part.weight)}</td>
                        <td>{part.trayId}</td>
                        <td><span className={`status-badge ${part.productId === null ? 'status-available' : 'status-consumed'}`}>
                            {part.productId === null ? 'Available' : `Product ${part.productId}`}
                        </span></td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}
