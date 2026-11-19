import InventoryBrowser from '../components/InventoryBrowser';
import type { Part } from '../types/part';
import type { Tray } from '../types/tray';
import { formatWeight, totalWeight } from '../utils/format';

interface InventoryPageProps {
    selectedParts: Part[];
    onTogglePart: (part: Part) => void;
    onReconcileTrays: (trays: Tray[]) => void;
    refreshKey: number;
    onRefresh: () => void;
    onCreateProduct: () => void;
}

export default function InventoryPage(props: InventoryPageProps) {
    return (
        <section>
            <div className="section-heading">
                <h2>Received inventory</h2>
                <p>Browse sealed trays and select parts for packaging.</p>
            </div>
            {props.selectedParts.length > 0 && <div className="selection-summary toolbar" role="status">
                <span>{props.selectedParts.length} parts selected · {formatWeight(totalWeight(props.selectedParts))}</span>
                <button type="button" className="button-primary" onClick={props.onCreateProduct}>Review selection</button>
            </div>}
            <InventoryBrowser {...props} />
        </section>
    );
}
