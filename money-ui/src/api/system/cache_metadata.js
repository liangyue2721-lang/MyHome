import request from '@/utils/request'

// 查询缓存元数据列表
export function listCache_metadata(query) {
  return request({
    url: '/system/cache_metadata/list',
    method: 'get',
    params: query
  })
}

// 查询缓存元数据详细
export function getCache_metadata(id) {
  return request({
    url: '/system/cache_metadata/' + id,
    method: 'get'
  })
}

// 新增缓存元数据
export function addCache_metadata(data) {
  return request({
    url: '/system/cache_metadata',
    method: 'post',
    data: data
  })
}

// 修改缓存元数据
export function updateCache_metadata(data) {
  return request({
    url: '/system/cache_metadata',
    method: 'put',
    data: data
  })
}

// 删除缓存元数据
export function delCache_metadata(id) {
  return request({
    url: '/system/cache_metadata/' + id,
    method: 'delete'
  })
}
