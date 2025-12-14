import request from '@/utils/request'

// 查询卖出价位提醒列表
export function listSell_price_alerts(query) {
  return request({
    url: '/stock/sell_price_alerts/list',
    method: 'get',
    params: query
  })
}

// 查询卖出价位提醒详细
export function getSell_price_alerts(id) {
  return request({
    url: '/stock/sell_price_alerts/' + id,
    method: 'get'
  })
}

// 新增卖出价位提醒
export function addSell_price_alerts(data) {
  return request({
    url: '/stock/sell_price_alerts',
    method: 'post',
    data: data
  })
}

// 修改卖出价位提醒
export function updateSell_price_alerts(data) {
  return request({
    url: '/stock/sell_price_alerts',
    method: 'put',
    data: data
  })
}

// 删除卖出价位提醒
export function delSell_price_alerts(id) {
  return request({
    url: '/stock/sell_price_alerts/' + id,
    method: 'delete'
  })
}
