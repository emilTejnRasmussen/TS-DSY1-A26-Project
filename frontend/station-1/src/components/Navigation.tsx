import type { Page } from '../App';

interface NavigationProps {
    activePage: Page;
    onPageChange: (page: Page) => void;
}

const pages: { id: Page; label: string }[] = [
    { id: 'registration', label: 'Registration' },
    { id: 'animals', label: 'Animals' },
    { id: 'farms', label: 'Farms' },
];

export default function Navigation({ activePage, onPageChange }: NavigationProps) {
    return (
        <nav className="navigation" aria-label="Main navigation">
            {pages.map(page => (
                <button
                    key={page.id}
                    type="button"
                    className={`nav-item ${activePage === page.id ? 'active' : ''}`}
                    onClick={() => onPageChange(page.id)}
                    aria-current={activePage === page.id ? 'page' : undefined}
                >
                    {page.label}
                </button>
            ))}
        </nav>
    );
}