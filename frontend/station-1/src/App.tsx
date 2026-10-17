import {useState} from 'react';
import Navigation from './components/Navigation';
import RegistrationPage from './pages/RegistrationPage';
import AnimalsPage from './pages/AnimalsPage';
import FarmsPage from './pages/FarmsPage';

export type Page = 'registration' | 'animals' | 'farms';

export default function App() {
    const [activePage, setActivePage] = useState<Page>('registration');

    function renderPage() {
        switch (activePage) {
            case 'registration':
                return <RegistrationPage/>;
            case 'animals':
                return <AnimalsPage/>;
            case 'farms':
                return <FarmsPage/>;
        }
    }

    return (
        <>
            <header className="app-header">
                <div className="header-content">
                    <div>
                        <span className="eyebrow">SLAUGHTERHOUSE / STATION 01</span>
                        <h1>Animal registration</h1>
                    </div>

                    <span className="station-label">Station 01</span>
                </div>
            </header>

            <main className="app-container">
                <Navigation
                    activePage={activePage}
                    onPageChange={setActivePage}
                />

                <div className="page-content">
                    {renderPage()}
                </div>
            </main>
        </>
    );
}