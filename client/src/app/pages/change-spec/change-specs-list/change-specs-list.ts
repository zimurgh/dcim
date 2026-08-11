import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AgGridAngular } from 'ag-grid-angular';
import type {
  ColDef,
  GetDetailRowDataParams,
  IDetailCellRendererParams,
} from 'ag-grid-community';
import { themeBalham } from 'ag-grid-community';
import { catchError, forkJoin, map, of } from 'rxjs';
import { ChangeApi, ChangeSpecApi, toChangeRow, type ChangeRow } from '../change-api';

export type ChangeSpecRow = {
  changeSpecId: number;
  name: string | null;
  ownerFirmId: number;
  ownerFirmName: string;
  status: string;
  changeCount: number;
  createdAt: string;
  createdBy: string | null;
  changes: ChangeRow[];
};

function byLatestFirst(a: ChangeRow, b: ChangeRow): number {
  return b.createdOrStagedAt.localeCompare(a.createdOrStagedAt);
}

@Component({
  selector: 'app-change-specs-list-page',
  imports: [AgGridAngular],
  templateUrl: './change-specs-list.html',
})
export class ChangeSpecsListPage {
  private readonly changeSpecs = inject(ChangeSpecApi);
  private readonly changes = inject(ChangeApi);

  protected readonly theme = themeBalham;
  protected readonly masterDetail = true;
  protected readonly detailRowAutoHeight = true;

  protected readonly columnDefs: ColDef<ChangeSpecRow>[] = [
    {
      field: 'changeSpecId',
      headerName: 'Spec ID',
      maxWidth: 130,
      cellRenderer: 'agGroupCellRenderer',
    },
    { field: 'name', headerName: 'Name' },
    { field: 'ownerFirmName', headerName: 'Owner Firm' },
    { field: 'status', headerName: 'Status', maxWidth: 160 },
    { field: 'changeCount', headerName: 'Changes', maxWidth: 110 },
    { field: 'createdAt', headerName: 'Created' },
    { field: 'createdBy', headerName: 'Created By', maxWidth: 140 },
  ];

  protected readonly rowData = toSignal(
    forkJoin({
      specs: this.changeSpecs.listAll(),
      changes: this.changes.listAll(),
    }).pipe(
      map(({ specs, changes }) => {
        const byId = new Map(changes.map((change) => [change.changeId, toChangeRow(change)]));
        return specs.map((spec) => {
          const members = spec.changeIds
            .map((id) => byId.get(id))
            .filter((row): row is ChangeRow => row != null)
            .sort(byLatestFirst);
          return {
            changeSpecId: spec.changeSpecId,
            name: spec.name,
            ownerFirmId: spec.ownerFirmId,
            ownerFirmName: spec.ownerFirmName,
            status: spec.status,
            createdAt: spec.createdAt,
            createdBy: spec.createdBy,
            changes: members,
            changeCount: members.length,
          } satisfies ChangeSpecRow;
        });
      }),
      catchError((err) => {
        console.error('Failed to load change specs', err);
        return of([] as ChangeSpecRow[]);
      }),
    ),
    { initialValue: [] as ChangeSpecRow[] },
  );

  protected readonly defaultColDef: ColDef = {
    flex: 1,
    minWidth: 100,
    sortable: true,
    filter: true,
    resizable: true,
  };

  protected readonly detailCellRendererParams = {
    detailGridOptions: {
      theme: themeBalham,
      columnDefs: [
        { field: 'changeId', headerName: 'Change ID', maxWidth: 120 },
        { field: 'stage', headerName: 'Stage', maxWidth: 130 },
        { field: 'statusLabel', headerName: 'Status', maxWidth: 150 },
        { field: 'assetType', headerName: 'Asset Type' },
        { field: 'action', headerName: 'Action', maxWidth: 130 },
        { field: 'assetIdentityId', headerName: 'Asset ID', maxWidth: 110 },
        { field: 'actor', headerName: 'Actor', maxWidth: 140 },
        { field: 'appliedByName', headerName: 'Applied By', maxWidth: 140 },
        { field: 'createdOrStagedAt', headerName: 'Timestamp' },
      ] as ColDef<ChangeRow>[],
      defaultColDef: {
        flex: 1,
        minWidth: 100,
        sortable: true,
        filter: true,
        resizable: true,
      },
    },
    getDetailRowData: (params: GetDetailRowDataParams<ChangeSpecRow, ChangeRow>) => {
      params.successCallback(params.data?.changes ?? []);
    },
  } as IDetailCellRendererParams<ChangeSpecRow, ChangeRow>;

  protected readonly isRowMaster = (dataItem: ChangeSpecRow) => dataItem.changes.length > 0;
}
