import request from '@/utils/request'

// 查询主力资金流向列表
export function listStockFlow(query) {
  return request({
    url: '/stock/stockFlow/list',
    method: 'get',
    params: query
  })
}

// 查询主力资金流向详细
export function getStockFlow(id) {
  return request({
    url: '/stock/stockFlow/' + id,
    method: 'get'
  })
}

// 新增主力资金流向
export function addStockFlow(data) {
  return request({
    url: '/stock/stockFlow',
    method: 'post',
    data: data
  })
}

// 修改主力资金流向
export function updateStockFlow(data) {
  return request({
    url: '/stock/stockFlow',
    method: 'put',
    data: data
  })
}

// 删除主力资金流向
export function delStockFlow(id) {
  return request({
    url: '/stock/stockFlow/' + id,
    method: 'delete'
  })
}
