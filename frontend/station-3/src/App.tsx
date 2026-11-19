import { useCallback, useState } from 'react';
import Navigation from './components/Navigation';
import InventoryPage from './pages/InventoryPage';
import CreateProductPage from './pages/CreateProductPage';
import ProductsPage from './pages/ProductsPage';
import type { Part } from './types/part';
import type { Tray } from './types/tray';

export type Page = 'inventory' | 'create-product' | 'products';

export default function App() {
    const [activePage, setActivePage] = useState<Page>('inventory');
    const [selectedParts, setSelectedParts] = useState<Part[]>([]);
    const [refreshKey, setRefreshKey] = useState(0);
    const [isBusy, setIsBusy] = useState(false);

    const togglePart = useCallback((part: Part) => {
        if (part.productId !== null) return;
        setSelectedParts(current => current.some(item => item.id === part.id)
            ? current.filter(item => item.id !== part.id) : [...current, part]);
    }, []);

    const reconcileTrays = useCallback((trays: Tray[]) => {
        setSelectedParts(current => current.flatMap(part => {
            const tray = trays.find(item => item.id === part.trayId);
            if (!tray) return [part];
            const refreshed = tray.parts.find(item => item.id === part.id && item.productId === null);
            return refreshed ? [refreshed] : [];
        }));
    }, []);

    const refreshInventory = useCallback(() => setRefreshKey(key => key + 1), []);

    function handleProductCreated() {
        setSelectedParts([]);
        refreshInventory();
    }

    return (
        <>
            <header className="app-header"><div className="header-content">
                <div><span className="eyebrow">SLAUGHTERHOUSE / STATION 03</span><h1>Packaging</h1></div>
                <span className="station-label">Station 03</span>
            </div></header>
            <main className="app-container">
                <Navigation activePage={activePage} onPageChange={setActivePage} disabled={isBusy} />
                <div className="page-content">
                    {activePage === 'inventory' && <InventoryPage selectedParts={selectedParts} onTogglePart={togglePart}
                        onReconcileTrays={reconcileTrays} refreshKey={refreshKey} onRefresh={refreshInventory}
                        onCreateProduct={() => setActivePage('create-product')} />}
                    {activePage === 'create-product' && <CreateProductPage selectedParts={selectedParts}
                        onTogglePart={togglePart} onSelectionChange={setSelectedParts} onReconcileTrays={reconcileTrays}
                        refreshKey={refreshKey} onRefresh={refreshInventory} onProductCreated={handleProductCreated}
                        isBusy={isBusy} onBusyChange={setIsBusy} />}
                    {activePage === 'products' && <ProductsPage />}
                </div>
            </main>
        </>
    );
}
