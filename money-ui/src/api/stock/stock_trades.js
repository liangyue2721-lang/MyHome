import request from '@/utils/request'

// 查询股票利润列表
export function listStock_trades(query) {
  return request({
    url: '/stock/stock_trades/list',
    method: 'get',
    params: query
  })
}

// 查询股票利润详细
export function getStock_trades(id) {
  return request({
    url: '/stock/stock_trades/' + id,
    method: 'get'
  })
}

// 新增股票利润
export function addStock_trades(data) {
  return request({
    url: '/stock/stock_trades',
    method: 'post',
    data: data
  })
}

// 修改股票利润
export function updateStock_trades(data) {
  return request({
    url: '/stock/stock_trades',
    method: 'put',
    data: data
  })
}

// 删除股票利润
export function delStock_trades(id) {
  return request({
    url: '/stock/stock_trades/' + id,
    method: 'delete'
  })
}
