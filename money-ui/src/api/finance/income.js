import request from '@/utils/request'

// 查询收入列表
export function listIncome(query) {
  return request({
    url: '/finance/income/list',
    method: 'get',
    params: query
  })
}

// 查询收入详细
export function getIncome(incomeId) {
  return request({
    url: '/finance/income/' + incomeId,
    method: 'get'
  })
}

// 新增收入
export function addIncome(data) {
  return request({
    url: '/finance/income',
    method: 'post',
    data: data
  })
}

// 修改收入
export function updateIncome(data) {
  return request({
    url: '/finance/income',
    method: 'put',
    data: data
  })
}

// 删除收入
export function delIncome(incomeId) {
  return request({
    url: '/finance/income/' + incomeId,
    method: 'delete'
  })
}

// 获取收入来源选项列表
export function getSourceOptions() {
  return request({
    url: '/finance/income/getSourceOptions',
    method: 'get'
  })
}
