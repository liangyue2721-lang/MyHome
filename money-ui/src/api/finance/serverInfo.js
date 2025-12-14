import request from '@/utils/request'

// 查询服务器有效期管理（MySQL5.7兼容版）列表
export function listServerInfo(query) {
  return request({
    url: '/finance/serverInfo/list',
    method: 'get',
    params: query
  })
}

// 查询服务器有效期管理（MySQL5.7兼容版）详细
export function getServerInfo(id) {
  return request({
    url: '/finance/serverInfo/' + id,
    method: 'get'
  })
}

// 新增服务器有效期管理（MySQL5.7兼容版）
export function addServerInfo(data) {
  return request({
    url: '/finance/serverInfo',
    method: 'post',
    data: data
  })
}

// 修改服务器有效期管理（MySQL5.7兼容版）
export function updateServerInfo(data) {
  return request({
    url: '/finance/serverInfo',
    method: 'put',
    data: data
  })
}

// 删除服务器有效期管理（MySQL5.7兼容版）
export function delServerInfo(id) {
  return request({
    url: '/finance/serverInfo/' + id,
    method: 'delete'
  })
}
