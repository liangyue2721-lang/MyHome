import request from '@/utils/request'

// 查询投资利润回报记录列表
export function listInvestment_records(query) {
  return request({
    url: '/finance/investment_records/list',
    method: 'get',
    params: query
  })
}

// 查询投资利润回报记录详细
export function getInvestment_records(id) {
  return request({
    url: '/finance/investment_records/' + id,
    method: 'get'
  })
}

// 新增投资利润回报记录
export function addInvestment_records(data) {
  return request({
    url: '/finance/investment_records',
    method: 'post',
    data: data
  })
}

// 修改投资利润回报记录
export function updateInvestment_records(data) {
  return request({
    url: '/finance/investment_records',
    method: 'put',
    data: data
  })
}

// 删除投资利润回报记录
export function delInvestment_records(id) {
  return request({
    url: '/finance/investment_records/' + id,
    method: 'delete'
  })
}
