import { Component, computed, signal } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import type { ICellRendererAngularComp } from 'ag-grid-angular';
import type { ColDef, ICellRendererParams } from 'ag-grid-community';
import { themeBalham } from 'ag-grid-community';
import type {
  CableRow,
  CrossConnectRow,
  DocumentRow,
  MarketDataFeedRow,
} from '../connectivity-demo-data';

type DetailTab = 'feeds' | 'cables' | 'documents';

type DetailTabItem = {
  id: DetailTab;
  label: string;
};

@Component({
  selector: 'app-cross-connect-detail',
  imports: [AgGridAngular],
  templateUrl: './cross-connect-detail.html',
  styles: `
    :host {
      display: block;
      height: 100%;
      padding: 0.75rem 1rem 1rem;
      box-sizing: border-box;
    }
  `,
})
export class CrossConnectDetail implements ICellRendererAngularComp {
  protected readonly theme = themeBalham;
  protected readonly activeTab = signal<DetailTab>('feeds');
  protected readonly crossConnect = signal<CrossConnectRow | null>(null);

  protected readonly tabs: DetailTabItem[] = [
    { id: 'feeds', label: 'Market Data Feeds' },
    { id: 'cables', label: 'Cables' },
    { id: 'documents', label: 'Documents' },
  ];

  protected readonly defaultColDef: ColDef = {
    flex: 1,
    minWidth: 100,
    sortable: true,
    filter: true,
    resizable: true,
  };

  private readonly feedColumnDefs: ColDef<MarketDataFeedRow>[] = [
    { field: 'marketDataFeedId', headerName: 'Feed ID', maxWidth: 110 },
    { field: 'marketDataFeedName', headerName: 'Name' },
    { field: 'marketDataFeedTypeName', headerName: 'Type', maxWidth: 140 },
    { field: 'ownerFirmName', headerName: 'Owner Firm' },
    { field: 'billingFirmName', headerName: 'Billing Firm' },
    { field: 'providerFirmName', headerName: 'Provider Firm' },
    { field: 'status', headerName: 'Status', maxWidth: 120 },
  ];

  private readonly cableColumnDefs: ColDef<CableRow>[] = [
    { field: 'cableId', headerName: 'Cable ID', maxWidth: 110 },
    { field: 'cableName', headerName: 'Name' },
    { field: 'portAId', headerName: 'Port A', maxWidth: 110 },
    { field: 'portBId', headerName: 'Port B', maxWidth: 110 },
    { field: 'status', headerName: 'Status', maxWidth: 120 },
  ];

  private readonly documentColumnDefs: ColDef<DocumentRow>[] = [
    { field: 'documentId', headerName: 'Document ID', maxWidth: 130 },
    { field: 'documentName', headerName: 'Name' },
    { field: 'status', headerName: 'Status', maxWidth: 120 },
  ];

  protected readonly columnDefs = computed<ColDef[]>(() => {
    switch (this.activeTab()) {
      case 'cables':
        return this.cableColumnDefs;
      case 'documents':
        return this.documentColumnDefs;
      default:
        return this.feedColumnDefs;
    }
  });

  protected readonly rowData = computed(() => {
    const row = this.crossConnect();
    if (!row) {
      return [];
    }
    switch (this.activeTab()) {
      case 'cables':
        return row.cables;
      case 'documents':
        return row.documents;
      default:
        return row.marketDataFeeds;
    }
  });

  agInit(params: ICellRendererParams<CrossConnectRow>): void {
    this.crossConnect.set(params.data ?? null);
    this.activeTab.set('feeds');
  }

  refresh(params: ICellRendererParams<CrossConnectRow>): boolean {
    this.crossConnect.set(params.data ?? null);
    return true;
  }

  protected selectTab(tab: DetailTab): void {
    this.activeTab.set(tab);
  }
}
