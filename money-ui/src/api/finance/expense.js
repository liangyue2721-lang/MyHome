import request from '@/utils/request'

// 查询消费列表
export function listExpense(query) {
  return request({
    url: '/finance/expense/list',
    method: 'get',
    params: query
  })
}

// 查询消费详细
export function getExpense(expenseId) {
  return request({
    url: '/finance/expense/' + expenseId,
    method: 'get'
  })
}

// 新增消费
export function addExpense(data) {
  return request({
    url: '/finance/expense',
    method: 'post',
    data: data
  })
}

// 修改消费
export function updateExpense(data) {
  return request({
    url: '/finance/expense',
    method: 'put',
    data: data
  })
}

// 删除消费
export function delExpense(expenseId) {
  return request({
    url: '/finance/expense/' + expenseId,
    method: 'delete'
  })
}

// 同步消费
export function syncExpense(expenseId) {
  return request({
    url: '/finance/expense/syncExpense',
    method: 'get'
  })
}
