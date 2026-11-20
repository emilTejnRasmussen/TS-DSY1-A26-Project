import type { Page } from '../App';

interface NavigationProps {
    activePage: Page;
    onPageChange: (page: Page) => void;
}

export default function Navigation({ activePage, onPageChange }: NavigationProps) {
    const pages: { id: Page; label: string }[] = [{ id: 'parts', label: 'Parts' }, { id: 'trays', label: 'Trays' }];

    return (
        <nav className="navigation" aria-label="Main navigation">
            {pages.map(page => (
                <button key={page.id} type="button"
                    className={`nav-item ${activePage === page.id ? 'active' : ''}`}
                    aria-current={activePage === page.id ? 'page' : undefined}
                    onClick={() => onPageChange(page.id)}>{page.label}</button>
            ))}
        </nav>
    );
}
