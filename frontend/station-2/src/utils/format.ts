export function formatPartType(type: string): string {
    return type.charAt(0) + type.slice(1).toLowerCase().replaceAll('_', ' ');
}

export function formatWeight(weight: number): string {
    return `${weight.toLocaleString('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} kg`;
}

export function formatDate(value?: string | null): string {
    if (!value) return '—';
    return new Date(value).toLocaleString('en-GB', { dateStyle: 'short', timeStyle: 'short' });
}

export function totalWeight(parts: { weight: number }[]): number {
    return parts.reduce((total, part) => total + Math.round(part.weight * 100), 0) / 100;
}
