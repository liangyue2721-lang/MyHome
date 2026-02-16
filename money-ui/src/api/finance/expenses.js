import request from '@/utils/request'

// 查询婚礼订婚支出流水列表
export function listExpenses(query) {
  return request({
    url: '/finance/expenses/list',
    method: 'get',
    params: query
  })
}

// 查询婚礼订婚支出流水统计
export function getExpensesStats(query) {
  return request({
    url: '/finance/expenses/stats',
    method: 'get',
    params: query
  })
}

// 查询婚礼订婚支出流水详细
export function getExpenses(id) {
  return request({
    url: '/finance/expenses/' + id,
    method: 'get'
  })
}

// 新增婚礼订婚支出流水
export function addExpenses(data) {
  return request({
    url: '/finance/expenses',
    method: 'post',
    data: data
  })
}

// 修改婚礼订婚支出流水
export function updateExpenses(data) {
  return request({
    url: '/finance/expenses',
    method: 'put',
    data: data
  })
}

// 删除婚礼订婚支出流水
export function delExpenses(id) {
  return request({
    url: '/finance/expenses/' + id,
    method: 'delete'
  })
}
