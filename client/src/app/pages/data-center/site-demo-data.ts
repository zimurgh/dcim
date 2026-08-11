export type SiteAssetRow = {
  path: string[];
  name: string;
  assetType:
    | 'DATA_CENTER'
    | 'CAGE'
    | 'RACK'
    | 'RACK_DEVICE'
    | 'RACK_DEVICE_PORT';
  assetId: number;
  historyId: number;
  status: string;
};

export const SITE_ASSET_ROWS: SiteAssetRow[] = [
  {
    path: ['dc:1'],
    name: 'NY4',
    assetType: 'DATA_CENTER',
    assetId: 1,
    historyId: 101,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:10'],
    name: 'Cage A',
    assetType: 'CAGE',
    assetId: 10,
    historyId: 210,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:10', 'rack:20'],
    name: 'R-01',
    assetType: 'RACK',
    assetId: 20,
    historyId: 320,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:10', 'rack:20', 'device:30'],
    name: 'sw-ny4-a01',
    assetType: 'RACK_DEVICE',
    assetId: 30,
    historyId: 430,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:10', 'rack:20', 'device:30', 'port:40'],
    name: 'eth0',
    assetType: 'RACK_DEVICE_PORT',
    assetId: 40,
    historyId: 540,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:10', 'rack:20', 'device:30', 'port:41'],
    name: 'eth1',
    assetType: 'RACK_DEVICE_PORT',
    assetId: 41,
    historyId: 541,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:10', 'rack:21'],
    name: 'R-02',
    assetType: 'RACK',
    assetId: 21,
    historyId: 321,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:10', 'rack:21', 'device:31'],
    name: 'srv-ny4-a02',
    assetType: 'RACK_DEVICE',
    assetId: 31,
    historyId: 431,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:11'],
    name: 'Cage B',
    assetType: 'CAGE',
    assetId: 11,
    historyId: 211,
    status: 'Active',
  },
  {
    path: ['dc:1', 'cage:11', 'rack:22'],
    name: 'R-14',
    assetType: 'RACK',
    assetId: 22,
    historyId: 322,
    status: 'Terminated',
  },
  {
    path: ['dc:2'],
    name: 'LD4',
    assetType: 'DATA_CENTER',
    assetId: 2,
    historyId: 102,
    status: 'Active',
  },
  {
    path: ['dc:2', 'cage:12'],
    name: 'Cage 1',
    assetType: 'CAGE',
    assetId: 12,
    historyId: 212,
    status: 'Active',
  },
  {
    path: ['dc:2', 'cage:12', 'rack:23'],
    name: 'R-01',
    assetType: 'RACK',
    assetId: 23,
    historyId: 323,
    status: 'Active',
  },
  {
    path: ['dc:2', 'cage:12', 'rack:23', 'device:32'],
    name: 'sw-ld4-01',
    assetType: 'RACK_DEVICE',
    assetId: 32,
    historyId: 432,
    status: 'Active',
  },
  {
    path: ['dc:2', 'cage:12', 'rack:23', 'device:32', 'port:42'],
    name: 'ge-0/0/0',
    assetType: 'RACK_DEVICE_PORT',
    assetId: 42,
    historyId: 542,
    status: 'Active',
  },
];
