import request from '@/utils/request'

// 查询美股阶段行情信息列表
export function listUs(query) {
  return request({
    url: '/stock/us/list',
    method: 'get',
    params: query
  })
}

// 查询美股阶段行情信息详细
export function getUs(id) {
  return request({
    url: '/stock/us/' + id,
    method: 'get'
  })
}

// 新增美股阶段行情信息
export function addUs(data) {
  return request({
    url: '/stock/us',
    method: 'post',
    data: data
  })
}

// 修改美股阶段行情信息
export function updateUs(data) {
  return request({
    url: '/stock/us',
    method: 'put',
    data: data
  })
}

// 删除美股阶段行情信息
export function delUs(id) {
  return request({
    url: '/stock/us/' + id,
    method: 'delete'
  })
}
