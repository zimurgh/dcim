import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type ChangeDto = {
  changeId: number;
  stage: string;
  statusLabel: string;
  body: string | null;
  assetType: string | null;
  action: string | null;
  assetIdentityId: number | null;
  baseHistoryId: number | null;
  payloadId: number | null;
  createdOrStagedAt: string;
  actor: string | null;
  appliedBy: number | null;
  appliedByName: string | null;
  changeSpecId: number | null;
};

export type ChangeSpecDto = {
  changeSpecId: number;
  ownerFirmId: number;
  ownerFirmName: string;
  name: string | null;
  status: string;
  createdAt: string;
  createdBy: string | null;
  changeIds: number[];
  chrecs: Array<{
    chrecId: number;
    jiraKey: string;
    title: string | null;
    url: string | null;
  }>;
};

export type ChangeRow = {
  changeId: number;
  stage: string;
  statusLabel: string;
  assetType: string | null;
  action: string | null;
  assetIdentityId: number | null;
  actor: string | null;
  appliedByName: string | null;
  changeSpecId: number | null;
  createdOrStagedAt: string;
};

export function toChangeRow(dto: ChangeDto): ChangeRow {
  return {
    changeId: dto.changeId,
    stage: dto.stage,
    statusLabel: dto.statusLabel,
    assetType: dto.assetType,
    action: dto.action,
    assetIdentityId: dto.assetIdentityId,
    actor: dto.actor,
    appliedByName: dto.appliedByName,
    changeSpecId: dto.changeSpecId,
    createdOrStagedAt: dto.createdOrStagedAt,
  };
}

@Injectable({ providedIn: 'root' })
export class ChangeApi {
  private readonly http = inject(HttpClient);

  listAll(): Observable<ChangeDto[]> {
    return this.http.get<ChangeDto[]>('/api/changes');
  }
}

@Injectable({ providedIn: 'root' })
export class ChangeSpecApi {
  private readonly http = inject(HttpClient);

  listAll(): Observable<ChangeSpecDto[]> {
    return this.http.get<ChangeSpecDto[]>('/api/change-specs');
  }
}
