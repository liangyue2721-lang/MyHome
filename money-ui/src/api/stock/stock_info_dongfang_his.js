import request from '@/utils/request'

// 查询东方财富历史列表
export function listStock_info_dongfang_his(query) {
  return request({
    url: '/stock/stock_info_dongfang_his/list',
    method: 'get',
    params: query
  })
}

// 查询东方财富历史详细
export function getStock_info_dongfang_his(id) {
  return request({
    url: '/stock/stock_info_dongfang_his/' + id,
    method: 'get'
  })
}

// 新增东方财富历史
export function addStock_info_dongfang_his(data) {
  return request({
    url: '/stock/stock_info_dongfang_his',
    method: 'post',
    data: data
  })
}

// 修改东方财富历史
export function updateStock_info_dongfang_his(data) {
  return request({
    url: '/stock/stock_info_dongfang_his',
    method: 'put',
    data: data
  })
}

// 删除东方财富历史
export function delStock_info_dongfang_his(id) {
  return request({
    url: '/stock/stock_info_dongfang_his/' + id,
    method: 'delete'
  })
}
