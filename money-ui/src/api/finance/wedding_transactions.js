import request from '@/utils/request'

// 查询婚礼收支明细列表
export function listWeddingTransactions(query) {
  return request({
    url: '/finance/wedding_transactions/list',
    method: 'get',
    params: query
  })
}

// 查询婚礼收支明细详细
export function getWedding_transactions(id) {
  return request({
    url: '/finance/wedding_transactions/' + id,
    method: 'get'
  })
}

// 新增婚礼收支明细
export function addWeddingTransactions(data) {
  return request({
    url: '/finance/wedding_transactions',
    method: 'post',
    data: data
  })
}

// 修改婚礼收支明细
export function updateWeddingTransactions(data) {
  return request({
    url: '/finance/wedding_transactions',
    method: 'put',
    data: data
  })
}

// 删除婚礼收支明细
export function delWeddingTransactions(id) {
  return request({
    url: '/finance/wedding_transactions/' + id,
    method: 'delete'
  })
}
