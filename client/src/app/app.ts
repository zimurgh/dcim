import { Component, signal } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import type { ColDef } from 'ag-grid-community';
import { themeQuartz } from 'ag-grid-community';

type DemoRow = {
  asset: string;
  status: string;
  historyId: number;
};

@Component({
  selector: 'app-root',
  imports: [AgGridAngular],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('DCIM');
  protected readonly theme = themeQuartz;

  protected readonly columnDefs: ColDef<DemoRow>[] = [
    { field: 'asset', headerName: 'Asset' },
    { field: 'status', headerName: 'Status' },
    { field: 'historyId', headerName: 'History ID' },
  ];

  protected readonly rowData: DemoRow[] = [
    { asset: 'a1', status: 'staged', historyId: 1 },
    { asset: 'a2', status: 'modified', historyId: 3 },
  ];

  protected readonly defaultColDef: ColDef = {
    flex: 1,
    minWidth: 120,
  };
}
