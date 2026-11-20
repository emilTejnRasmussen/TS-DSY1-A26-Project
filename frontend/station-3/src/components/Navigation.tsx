import type { Page } from '../App';

interface NavigationProps {
    activePage: Page;
    onPageChange: (page: Page) => void;
    disabled: boolean;
}

const pages: { id: Page; label: string }[] = [
    { id: 'inventory', label: 'Inventory' },
    { id: 'create-product', label: 'Create product' },
    { id: 'products', label: 'Products' },
];

export default function Navigation({ activePage, onPageChange, disabled }: NavigationProps) {
    return (
        <nav className="navigation" aria-label="Main navigation">
            {pages.map(page => (
                <button key={page.id} type="button" disabled={disabled}
                    className={`nav-item ${activePage === page.id ? 'active' : ''}`}
                    aria-current={activePage === page.id ? 'page' : undefined}
                    onClick={() => onPageChange(page.id)}>{page.label}</button>
            ))}
        </nav>
    );
}
