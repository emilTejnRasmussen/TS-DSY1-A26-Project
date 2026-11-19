import { useState } from 'react';
import Navigation from './components/Navigation';
import PartsPage from './pages/PartsPage';
import TraysPage from './pages/TraysPage';

export type Page = 'parts' | 'trays';

export default function App() {
    const [activePage, setActivePage] = useState<Page>('parts');

    return (
        <>
            <header className="app-header">
                <div className="header-content">
                    <div>
                        <span className="eyebrow">SLAUGHTERHOUSE / STATION 02</span>
                        <h1>Animal processing</h1>
                    </div>
                    <span className="station-label">Station 02</span>
                </div>
            </header>
            <main className="app-container">
                <Navigation activePage={activePage} onPageChange={setActivePage} />
                <div className="page-content">
                    {activePage === 'parts' ? <PartsPage /> : <TraysPage />}
                </div>
            </main>
        </>
    );
}
