import { Component } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import type { ColDef } from 'ag-grid-community';
import { themeBalham } from 'ag-grid-community';
import { CROSS_CONNECT_ROWS, type CrossConnectRow } from './connectivity-demo-data';
import { CrossConnectDetail } from './cross-connect-detail/cross-connect-detail';

@Component({
  selector: 'app-connectivity-page',
  imports: [AgGridAngular],
  templateUrl: './connectivity.html',
})
export class ConnectivityPage {
  protected readonly theme = themeBalham;
  protected readonly masterDetail = true;
  protected readonly detailRowHeight = 340;
  protected readonly detailCellRenderer = CrossConnectDetail;

  protected readonly columnDefs: ColDef<CrossConnectRow>[] = [
    {
      field: 'crossConnectId',
      headerName: 'XC ID',
      maxWidth: 110,
      cellRenderer: 'agGroupCellRenderer',
    },
    { field: 'crossConnectName', headerName: 'Name' },
    { field: 'circuitId', headerName: 'Circuit ID', maxWidth: 140 },
    { field: 'crossConnectTypeName', headerName: 'Type', maxWidth: 160 },
    { field: 'latencyName', headerName: 'Latency', maxWidth: 110 },
    { field: 'speedName', headerName: 'Speed', maxWidth: 100 },
    { field: 'marketSegmentName', headerName: 'Market Segment', maxWidth: 140 },
    { field: 'ownerFirmName', headerName: 'Owner Firm' },
    { field: 'billingFirmName', headerName: 'Billing Firm' },
    { field: 'providerFirmName', headerName: 'Provider Firm' },
    { field: 'status', headerName: 'Status', maxWidth: 120 },
  ];

  protected readonly rowData: CrossConnectRow[] = CROSS_CONNECT_ROWS;

  protected readonly defaultColDef: ColDef = {
    flex: 1,
    minWidth: 100,
    sortable: true,
    filter: true,
    resizable: true,
  };

  protected readonly isRowMaster = (dataItem: CrossConnectRow) =>
    dataItem.marketDataFeeds.length > 0 ||
    dataItem.cables.length > 0 ||
    dataItem.documents.length > 0;
}
