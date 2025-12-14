import request from '@/utils/request'

// 查询指标信息列表
export function listIndicators(query) {
  return request({
    url: '/finance/indicators/list',
    method: 'get',
    params: query
  })
}

// 查询指标信息详细
export function getIndicators(id) {
  return request({
    url: '/finance/indicators/' + id,
    method: 'get'
  })
}

// 新增指标信息
export function addIndicators(data) {
  return request({
    url: '/finance/indicators',
    method: 'post',
    data: data
  })
}

// 修改指标信息
export function updateIndicators(data) {
  return request({
    url: '/finance/indicators',
    method: 'put',
    data: data
  })
}

// 删除指标信息
export function delIndicators(id) {
  return request({
    url: '/finance/indicators/' + id,
    method: 'delete'
  })
}
