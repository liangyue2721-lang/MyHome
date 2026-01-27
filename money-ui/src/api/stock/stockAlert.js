import request from '@/utils/request'

// 查询大资金入场异动预警列表
export function listStockAlert(query) {
  return request({
    url: '/stock/stockAlert/list',
    method: 'get',
    params: query
  })
}

// 查询大资金入场异动预警详细
export function getStockAlert(id) {
  return request({
    url: '/stock/stockAlert/' + id,
    method: 'get'
  })
}

// 新增大资金入场异动预警
export function addStockAlert(data) {
  return request({
    url: '/stock/stockAlert',
    method: 'post',
    data: data
  })
}

// 修改大资金入场异动预警
export function updateStockAlert(data) {
  return request({
    url: '/stock/stockAlert',
    method: 'put',
    data: data
  })
}

// 删除大资金入场异动预警
export function delStockAlert(id) {
  return request({
    url: '/stock/stockAlert/' + id,
    method: 'delete'
  })
}
