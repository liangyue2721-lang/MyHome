"""
诊断脚本: 测试eastmoney stock/get接口返回哪些字段有五档盘口数据
运行方式: python test_stock_fields.py
"""
import requests
import json

STOCK_URL = "https://push2.eastmoney.com/api/qt/stock/get"

# 测试所有可能包含五档数据的字段范围
# 已知: f167-f186 是资金流向(5日/10日主力净额), 不是五档盘口
FIELDS_TO_TEST = (
    # 基础已确认字段
    "f43,f44,f45,f46,f47,f48,f57,f58,f60,f71,"
    # 可能的五档字段(从东财网页源码中提取)
    "f19,f20,f17,f18,f31,f32,f33,f34,f35,f36,f37,f38,f39,f40,"
    # f530系列(部分源码引用)
    "f530,f531,f532,f533,f534,f535,f536,f537,f538,"
    # 委比/外盘/内盘
    "f49,f161,f168,"
    # f135-f149 (stock/get上下文)
    "f135,f136,f137,f138,f139,f140,f141,f142,f143,f144,f145,f146,f147,f148,f149,"
    # 资金流向(验证是否确实是资金流)
    "f167,f183,f184,f185,f186"
)

# 测试3只股票: 贵州茅台(沪) 平安银行(深) 比亚迪(深)
STOCKS = [
    ("1.600519", "贵州茅台"),
    ("0.000001", "平安银行"),
    ("0.002594", "比亚迪"),
]

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Referer": "https://quote.eastmoney.com/",
}

for secid, name in STOCKS:
    params = {
        'secid': secid,
        'fields': FIELDS_TO_TEST,
        'ut': 'fa5fd1943c7b386f172d6893dbfba10b',
        'fltt': 2,
        'invt': 2,
    }
    resp = requests.get(STOCK_URL, headers=headers, params=params, timeout=10)
    data = resp.json()

    print(f"\n{'='*60}")
    print(f"股票: {name} ({secid})")
    print(f"{'='*60}")

    if data.get('data'):
        d = data['data']
        # 按字段分组打印
        print(f"\n--- 基础字段 ---")
        print(f"  f43(最新价)={d.get('f43')} f44(最高)={d.get('f44')} f45(最低)={d.get('f45')}")
        print(f"  f46(今开)={d.get('f46')} f60(昨收)={d.get('f60')} f71(涨跌幅)={d.get('f71')}")
        print(f"  f47(成交量)={d.get('f47')} f48(成交额)={d.get('f48')}")
        print(f"  f57(代码)={d.get('f57')} f58(名称)={d.get('f58')}")

        print(f"\n--- 可能五档字段(f19/f20/f17/f18/f31-f40) ---")
        for f in ['f17','f18','f19','f20','f31','f32','f33','f34','f35','f36','f37','f38','f39','f40']:
            v = d.get(f)
            if v is not None and v != '-':
                print(f"  {f} = {v}")

        print(f"\n--- f530系列 ---")
        for f in ['f530','f531','f532','f533','f534','f535','f536','f537','f538']:
            v = d.get(f)
            if v is not None and v != '-':
                print(f"  {f} = {v}")

        print(f"\n--- f135-f149 ---")
        for i in range(135, 150):
            f = f'f{i}'
            v = d.get(f)
            if v is not None and v != '-':
                print(f"  {f} = {v}")

        print(f"\n--- 其他 ---")
        print(f"  f49(量比)={d.get('f49')} f161={d.get('f161')} f168(换手率)={d.get('f168')}")
        print(f"  f167={d.get('f167')} f183={d.get('f183')} f184={d.get('f184')}")

        # 打印所有非空字段
        print(f"\n--- 所有非空字段汇总 ---")
        non_empty = {k: v for k, v in d.items() if v is not None and v != '-' and v != ''}
        for k, v in sorted(non_empty.items(), key=lambda x: int(x[0][1:]) if x[0][1:].isdigit() else 999):
            print(f"  {k} = {v}")
    else:
        print(f"  无数据返回: {data}")

    print()
