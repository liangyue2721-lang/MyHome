import re

# 字段信息
# stock_code varchar(10) NOT NULL COMMENT '股票代码，例如 600519'
# market varchar(10) DEFAULT NULL COMMENT '市场标识，如 SH、SZ'
# task_status int DEFAULT NULL COMMENT '执行状态'
# execute_time datetime DEFAULT NULL COMMENT '计划执行时间'
# node_id int DEFAULT NULL COMMENT '节点ID'
# create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间'
# update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间'

def update_sql_file(file_path):
    # 读取文件内容
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()
    
    # 使用正则表达式匹配INSERT语句并替换为包含字段名的格式
    # 匹配模式：INSERT INTO `stock_kline_task` VALUES (值列表);
    pattern = r"INSERT INTO `stock_kline_task` VALUES \(([^)]+)\);"
    replacement = r"INSERT INTO `stock_kline_task` (stock_code, market, task_status, execute_time, node_id, create_time, update_time) VALUES (\1);"
    modified_content = re.sub(pattern, replacement, content)
    
    # 将修改后的内容写回原文件
    with open(file_path, 'w', encoding='utf-8') as file:
        file.write(modified_content)
    
    print(f"已成功处理文件: {file_path}")

if __name__ == "__main__":
    file_path = "stock_kline_task.sql"
    update_sql_file(file_path)
    print("SQL文件已更新为包含字段名的格式")