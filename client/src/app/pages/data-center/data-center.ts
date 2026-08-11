import { Component } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import type { ColDef, GetDataPath, ValueFormatterParams } from 'ag-grid-community';
import { themeBalham } from 'ag-grid-community';
import { SITE_ASSET_ROWS, type SiteAssetRow } from './site-demo-data';

const ASSET_TYPE_LABELS: Record<SiteAssetRow['assetType'], string> = {
  DATA_CENTER: 'Data Center',
  CAGE: 'Cage',
  RACK: 'Rack',
  RACK_DEVICE: 'Rack Device',
  RACK_DEVICE_PORT: 'Port',
};

@Component({
  selector: 'app-data-center-page',
  imports: [AgGridAngular],
  templateUrl: './data-center.html',
})
export class DataCenterPage {
  protected readonly theme = themeBalham;
  protected readonly treeData = true;
  protected readonly groupDefaultExpanded = 1;

  protected readonly getDataPath: GetDataPath<SiteAssetRow> = (data) => data.path;

  protected readonly autoGroupColumnDef: ColDef<SiteAssetRow> = {
    headerName: 'Asset',
    field: 'name',
    minWidth: 280,
    flex: 2,
    cellRendererParams: {
      suppressCount: true,
    },
  };

  protected readonly columnDefs: ColDef<SiteAssetRow>[] = [
    {
      field: 'assetType',
      headerName: 'Type',
      maxWidth: 160,
      valueFormatter: (p: ValueFormatterParams<SiteAssetRow, SiteAssetRow['assetType']>) =>
        p.value ? ASSET_TYPE_LABELS[p.value] : '',
    },
    { field: 'assetId', headerName: 'Asset ID', maxWidth: 110 },
    { field: 'status', headerName: 'Status', maxWidth: 130 },
    { field: 'historyId', headerName: 'History ID', maxWidth: 120 },
  ];

  protected readonly rowData: SiteAssetRow[] = SITE_ASSET_ROWS;

  protected readonly defaultColDef: ColDef = {
    flex: 1,
    minWidth: 100,
    sortable: true,
    filter: true,
    resizable: true,
  };
}
