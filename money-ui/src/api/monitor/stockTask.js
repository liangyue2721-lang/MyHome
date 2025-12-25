import request from '@/utils/request'

// 查询股票刷新任务状态列表
export function listStockTask(query) {
  return request({
    url: '/monitor/stock-task/list',
    method: 'get',
    params: query
  })
}
