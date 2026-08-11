import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AgGridAngular } from 'ag-grid-angular';
import type { ColDef } from 'ag-grid-community';
import { themeBalham } from 'ag-grid-community';
import { catchError, map, of } from 'rxjs';
import { ChangeApi, toChangeRow, type ChangeRow } from '../change-api';

export type { ChangeRow };

@Component({
  selector: 'app-changelog-page',
  imports: [AgGridAngular],
  templateUrl: './changelog.html',
})
export class ChangelogPage {
  private readonly changes = inject(ChangeApi);

  protected readonly theme = themeBalham;

  protected readonly columnDefs: ColDef<ChangeRow>[] = [
    { field: 'changeId', headerName: 'Change ID', maxWidth: 120 },
    { field: 'stage', headerName: 'Stage', maxWidth: 130 },
    { field: 'statusLabel', headerName: 'Status', maxWidth: 150 },
    { field: 'assetType', headerName: 'Asset Type' },
    { field: 'action', headerName: 'Action', maxWidth: 130 },
    { field: 'assetIdentityId', headerName: 'Asset ID', maxWidth: 110 },
    { field: 'changeSpecId', headerName: 'Spec ID', maxWidth: 110 },
    { field: 'actor', headerName: 'Actor', maxWidth: 140 },
    { field: 'appliedByName', headerName: 'Applied By', maxWidth: 140 },
    { field: 'createdOrStagedAt', headerName: 'Timestamp' },
  ];

  protected readonly rowData = toSignal(
    this.changes.listAll().pipe(
      map((rows) => rows.map(toChangeRow)),
      catchError((err) => {
        console.error('Failed to load changelog', err);
        return of([] as ChangeRow[]);
      }),
    ),
    { initialValue: [] as ChangeRow[] },
  );

  protected readonly defaultColDef: ColDef = {
    flex: 1,
    minWidth: 100,
    sortable: true,
    filter: true,
    resizable: true,
  };
}
