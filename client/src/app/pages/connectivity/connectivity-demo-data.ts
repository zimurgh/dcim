export type MarketDataFeedRow = {
  marketDataFeedId: number;
  marketDataFeedHistoryId: number;
  marketDataFeedName: string;
  marketDataFeedTypeId: number;
  marketDataFeedTypeName: string;
  ownerFirmId: number;
  ownerFirmName: string;
  billingFirmId: number;
  billingFirmName: string;
  providerFirmId: number | null;
  providerFirmName: string | null;
  status: string;
};

export type CableRow = {
  cableId: number;
  cableHistoryId: number;
  cableName: string;
  portAId: number;
  portBId: number;
  status: string;
};

export type DocumentRow = {
  documentId: number;
  documentHistoryId: number;
  documentName: string;
  status: string;
};

export type CrossConnectRow = {
  crossConnectId: number;
  crossConnectHistoryId: number;
  crossConnectName: string;
  circuitId: string;
  crossConnectTypeId: number;
  crossConnectTypeName: string;
  latencyId: number;
  latencyName: string;
  speedId: number;
  speedName: string;
  marketSegmentId: number | null;
  marketSegmentName: string | null;
  ownerFirmId: number;
  ownerFirmName: string;
  billingFirmId: number;
  billingFirmName: string;
  providerFirmId: number | null;
  providerFirmName: string | null;
  status: string;
  marketDataFeeds: MarketDataFeedRow[];
  cables: CableRow[];
  documents: DocumentRow[];
};

export const CROSS_CONNECT_ROWS: CrossConnectRow[] = [
  {
    crossConnectId: 1,
    crossConnectHistoryId: 101,
    crossConnectName: 'NY4-XC-001',
    circuitId: 'CKT-1001',
    crossConnectTypeId: 2,
    crossConnectTypeName: 'Single-mode Fiber',
    latencyId: 1,
    latencyName: 'LL',
    speedId: 3,
    speedName: '10G',
    marketSegmentId: 5,
    marketSegmentName: 'Equities',
    ownerFirmId: 12,
    ownerFirmName: 'Acme Trading',
    billingFirmId: 12,
    billingFirmName: 'Acme Trading',
    providerFirmId: 4,
    providerFirmName: 'Equinix',
    status: 'Active',
    marketDataFeeds: [
      {
        marketDataFeedId: 11,
        marketDataFeedHistoryId: 211,
        marketDataFeedName: 'NYSE PITCH',
        marketDataFeedTypeId: 1,
        marketDataFeedTypeName: 'Depth of Book',
        ownerFirmId: 12,
        ownerFirmName: 'Acme Trading',
        billingFirmId: 12,
        billingFirmName: 'Acme Trading',
        providerFirmId: 4,
        providerFirmName: 'Equinix',
        status: 'Active',
      },
      {
        marketDataFeedId: 12,
        marketDataFeedHistoryId: 212,
        marketDataFeedName: 'NASDAQ ITCH',
        marketDataFeedTypeId: 2,
        marketDataFeedTypeName: 'ITCH',
        ownerFirmId: 12,
        ownerFirmName: 'Acme Trading',
        billingFirmId: 12,
        billingFirmName: 'Acme Trading',
        providerFirmId: 4,
        providerFirmName: 'Equinix',
        status: 'Active',
      },
    ],
    cables: [
      {
        cableId: 21,
        cableHistoryId: 321,
        cableName: 'MMF-A01',
        portAId: 40,
        portBId: 50,
        status: 'Active',
      },
      {
        cableId: 22,
        cableHistoryId: 322,
        cableName: 'MMF-A02',
        portAId: 41,
        portBId: 51,
        status: 'Active',
      },
    ],
    documents: [
      {
        documentId: 31,
        documentHistoryId: 431,
        documentName: 'LOA-NY4-XC-001.pdf',
        status: 'Active',
      },
      {
        documentId: 32,
        documentHistoryId: 432,
        documentName: 'Circuit Design.docx',
        status: 'Active',
      },
    ],
  },
  {
    crossConnectId: 2,
    crossConnectHistoryId: 102,
    crossConnectName: 'NY4-XC-014',
    circuitId: 'CKT-1014',
    crossConnectTypeId: 2,
    crossConnectTypeName: 'Single-mode Fiber',
    latencyId: 2,
    latencyName: 'ULL',
    speedId: 1,
    speedName: '1G',
    marketSegmentId: null,
    marketSegmentName: null,
    ownerFirmId: 7,
    ownerFirmName: 'Beta Capital',
    billingFirmId: 7,
    billingFirmName: 'Beta Capital',
    providerFirmId: null,
    providerFirmName: null,
    status: 'Active',
    marketDataFeeds: [
      {
        marketDataFeedId: 13,
        marketDataFeedHistoryId: 213,
        marketDataFeedName: 'CME MDP 3.0',
        marketDataFeedTypeId: 3,
        marketDataFeedTypeName: 'MDP 3.0',
        ownerFirmId: 7,
        ownerFirmName: 'Beta Capital',
        billingFirmId: 7,
        billingFirmName: 'Beta Capital',
        providerFirmId: null,
        providerFirmName: null,
        status: 'Active',
      },
    ],
    cables: [
      {
        cableId: 23,
        cableHistoryId: 323,
        cableName: 'SMF-B01',
        portAId: 60,
        portBId: 70,
        status: 'Active',
      },
    ],
    documents: [
      {
        documentId: 33,
        documentHistoryId: 433,
        documentName: 'LOA-NY4-XC-014.pdf',
        status: 'Active',
      },
    ],
  },
  {
    crossConnectId: 3,
    crossConnectHistoryId: 103,
    crossConnectName: 'LD4-XC-003',
    circuitId: 'CKT-2003',
    crossConnectTypeId: 1,
    crossConnectTypeName: 'Multimode Fiber',
    latencyId: 1,
    latencyName: 'LL',
    speedId: 2,
    speedName: '40G',
    marketSegmentId: 8,
    marketSegmentName: 'Futures',
    ownerFirmId: 12,
    ownerFirmName: 'Acme Trading',
    billingFirmId: 9,
    billingFirmName: 'Acme Billing Co',
    providerFirmId: 4,
    providerFirmName: 'Equinix',
    status: 'Terminated',
    marketDataFeeds: [],
    cables: [
      {
        cableId: 24,
        cableHistoryId: 324,
        cableName: 'MMF-C01',
        portAId: 80,
        portBId: 90,
        status: 'Terminated',
      },
    ],
    documents: [
      {
        documentId: 34,
        documentHistoryId: 434,
        documentName: 'Decommission Notice.pdf',
        status: 'Active',
      },
    ],
  },
];
