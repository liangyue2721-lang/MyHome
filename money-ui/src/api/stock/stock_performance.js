import request from '@/utils/request'

// 查询股票当年现数据列表
export function listStock_performance(query) {
  return request({
    url: '/stock/stock_performance/list',
    method: 'get',
    params: query
  })
}

// 查询股票当年现数据详细
export function getStock_performance(id) {
  return request({
    url: '/stock/stock_performance/' + id,
    method: 'get'
  })
}

// 新增股票当年现数据
export function addStock_performance(data) {
  return request({
    url: '/stock/stock_performance',
    method: 'post',
    data: data
  })
}

// 修改股票当年现数据
export function updateStock_performance(data) {
  return request({
    url: '/stock/stock_performance',
    method: 'put',
    data: data
  })
}

// 删除股票当年现数据
export function delStock_performance(id) {
  return request({
    url: '/stock/stock_performance/' + id,
    method: 'delete'
  })
}
