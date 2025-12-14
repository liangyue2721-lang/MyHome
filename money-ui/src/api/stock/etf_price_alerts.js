import request from '@/utils/request'

// 查询ETF买入卖出价格提醒列表
export function listEtf_price_alerts(query) {
  return request({
    url: '/stock/etf_price_alerts/list',
    method: 'get',
    params: query
  })
}

// 查询ETF买入卖出价格提醒详细
export function getEtf_price_alerts(id) {
  return request({
    url: '/stock/etf_price_alerts/' + id,
    method: 'get'
  })
}

// 新增ETF买入卖出价格提醒
export function addEtf_price_alerts(data) {
  return request({
    url: '/stock/etf_price_alerts',
    method: 'post',
    data: data
  })
}

// 修改ETF买入卖出价格提醒
export function updateEtf_price_alerts(data) {
  return request({
    url: '/stock/etf_price_alerts',
    method: 'put',
    data: data
  })
}

// 删除ETF买入卖出价格提醒
export function delEtf_price_alerts(id) {
  return request({
    url: '/stock/etf_price_alerts/' + id,
    method: 'delete'
  })
}
