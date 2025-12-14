import request from '@/utils/request'

// 查询买入价位提醒列表
export function listWatch_stock(query) {
  return request({
    url: '/stock/watch_stock/list',
    method: 'get',
    params: query
  })
}

// 查询买入价位提醒详细
export function getWatch_stock(id) {
  return request({
    url: '/stock/watch_stock/' + id,
    method: 'get'
  })
}

// 新增买入价位提醒
export function addWatch_stock(data) {
  return request({
    url: '/stock/watch_stock',
    method: 'post',
    data: data
  })
}

// 修改买入价位提醒
export function updateWatch_stock(data) {
  return request({
    url: '/stock/watch_stock',
    method: 'put',
    data: data
  })
}

// 删除买入价位提醒
export function delWatch_stock(id) {
  return request({
    url: '/stock/watch_stock/' + id,
    method: 'delete'
  })
}
