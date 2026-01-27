import request from '@/utils/request'

// 查询股票逐笔成交明细列表
export function listStockTick(query) {
  return request({
    url: '/stock/stockTick/list',
    method: 'get',
    params: query
  })
}

// 查询股票逐笔成交明细详细
export function getStockTick(id) {
  return request({
    url: '/stock/stockTick/' + id,
    method: 'get'
  })
}

// 新增股票逐笔成交明细
export function addStockTick(data) {
  return request({
    url: '/stock/stockTick',
    method: 'post',
    data: data
  })
}

// 修改股票逐笔成交明细
export function updateStockTick(data) {
  return request({
    url: '/stock/stockTick',
    method: 'put',
    data: data
  })
}

// 删除股票逐笔成交明细
export function delStockTick(id) {
  return request({
    url: '/stock/stockTick/' + id,
    method: 'delete'
  })
}
