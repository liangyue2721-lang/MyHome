import request from '@/utils/request'

// 查询交易分类关键词列表
export function listCategories(query) {
  return request({
    url: '/finance/categories/list',
    method: 'get',
    params: query
  })
}

// 查询交易分类关键词详细
export function getCategories(id) {
  return request({
    url: '/finance/categories/' + id,
    method: 'get'
  })
}

// 新增交易分类关键词
export function addCategories(data) {
  return request({
    url: '/finance/categories',
    method: 'post',
    data: data
  })
}

// 修改交易分类关键词
export function updateCategories(data) {
  return request({
    url: '/finance/categories',
    method: 'put',
    data: data
  })
}

// 删除交易分类关键词
export function delCategories(id) {
  return request({
    url: '/finance/categories/' + id,
    method: 'delete'
  })
}
