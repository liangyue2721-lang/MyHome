import request from '@/utils/request'

// 查询用户水电费缴纳记录列表
export function listPayments(query) {
  return request({
    url: '/finance/payments/list',
    method: 'get',
    params: query
  })
}

// 查询用户水电费缴纳记录详细
export function getPayments(id) {
  return request({
    url: '/finance/payments/' + id,
    method: 'get'
  })
}

// 新增用户水电费缴纳记录
export function addPayments(data) {
  return request({
    url: '/finance/payments',
    method: 'post',
    data: data
  })
}

// 修改用户水电费缴纳记录
export function updatePayments(data) {
  return request({
    url: '/finance/payments',
    method: 'put',
    data: data
  })
}

// 删除用户水电费缴纳记录
export function delPayments(id) {
  return request({
    url: '/finance/payments/' + id,
    method: 'delete'
  })
}
// 查询用户水电费缴纳记录列表
export function queryPayments(query) {
  return request({
    url: '/finance/payments/query',
    method: 'get',
    params: query
  })
}
