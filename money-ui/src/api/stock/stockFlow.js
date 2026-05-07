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

// 获取图表数据
export function getChartData() {
  return request({
    url: '/stock/stockFlow/chartData',
    method: 'get'
  })
}

// 获取背离数据
export function getDivergenceData() {
  return request({
    url: '/stock/stockFlow/divergenceData',
    method: 'get'
  })
}

// 获取指定股票的最新资金流向数据
export function getLatestFundFlowByCodes(codes) {
  return request({
    url: '/stock/stockFlow/latestByCodes',
    method: 'get',
    params: { codes: codes.join(',') }
  })
}

// 删除主力资金流向
export function delStockFlow(id) {
  return request({
    url: '/stock/stockFlow/' + id,
    method: 'delete'
  })
}
