"""端到端验证: tls_client + push2 API"""
import sys
sys.path.insert(0, '.')
from utils import _fetch_json

# 测试1: push2 竞价clist
print("=== push2: 竞价clist ===")
d = _fetch_json(
    'https://push2.eastmoney.com/api/qt/clist/get',
    params={'pn': 1, 'pz': 5, 'po': 1, 'np': 1, 'fltt': 2, 'invt': 2, 'fid': 'f3',
            'fs': 'm:0+t:6+f:!2', 'fields': 'f12,f14,f2,f3'},
)
if d and d.get('data'):
    items = d['data']['diff']
    print(f"  OK: {d['data']['total']} stocks, sample: {items[0].get('f12')} {items[0].get('f14')}")
else:
    print("  FAIL")

# 测试2: push2 盘口stock
print("\n=== push2: 盘口stock ===")
d = _fetch_json(
    'https://push2.eastmoney.com/api/qt/stock/get',
    params={'secid': '1.600519', 'fields': 'f43,f44,f45,f46,f47,f48,f57,f58,f168,f170'},
)
if d and d.get('data'):
    print(f"  OK: 贵州茅台 f43={d['data'].get('f43')}")
else:
    print("  FAIL")

# 测试3: push2his K线
print("\n=== push2his: K线 ===")
d = _fetch_json(
    'https://push2his.eastmoney.com/api/qt/stock/kline/get',
    params={'secid': '1.600519', 'fields1': 'f1,f2,f3,f4,f5,f6', 'fields2': 'f51,f52,f53,f54,f55,f56,f57',
            'klt': '101', 'fqt': '1', 'beg': '0', 'end': '20500101', 'lmt': '3'},
)
if d and d.get('data'):
    klines = d['data'].get('klines', [])
    print(f"  OK: {len(klines)} klines, latest: {klines[-1] if klines else 'N/A'}")
else:
    print("  FAIL")

# 测试4: push2ex 涨停池
print("\n=== push2ex: 涨停池 ===")
d = _fetch_json(
    'https://push2ex.eastmoney.com/getTopicZTPool',
    params={'ut': '7eea3edcaed734bea9cbfc24409ed989', 'dpt': 'wz.ztzt', 'Pageindex': 0, 'pagesize': 3,
            'date': '20260509'},
)
if d and d.get('data'):
    pool = d['data'].get('pool', [])
    print(f"  OK: {len(pool)} stocks in ZT pool")
else:
    print("  FAIL")

# 测试5: datacenter-web 龙虎榜
print("\n=== datacenter-web: 龙虎榜 ===")
d = _fetch_json(
    'https://datacenter-web.eastmoney.com/api/data/v1/get',
    params={'reportName': 'RPT_DMSK_TS_STOCKNEW', 'columns': 'ALL', 'pageNumber': 1, 'pageSize': 3,
            'sortColumns': 'SECURITY_CODE', 'sortTypes': 1},
)
if d and d.get('result') and d['result'].get('data'):
    items = d['result']['data']
    print(f"  OK: {d['result']['count']} records")
else:
    print(f"  code={d.get('code') if d else 'N/A'}")
