import request from '@/utils/request'

// 查询攒金收益记录列表
export function listGold_earnings(query) {
  return request({
    url: '/stock/gold_earnings/list',
    method: 'get',
    params: query
  })
}

// 查询攒金收益记录详细
export function getGold_earnings(id) {
  return request({
    url: '/stock/gold_earnings/' + id,
    method: 'get'
  })
}

// 新增攒金收益记录
export function addGold_earnings(data) {
  return request({
    url: '/stock/gold_earnings',
    method: 'post',
    data: data
  })
}

// 修改攒金收益记录
export function updateGold_earnings(data) {
  return request({
    url: '/stock/gold_earnings',
    method: 'put',
    data: data
  })
}

// 删除攒金收益记录
export function delGold_earnings(id) {
  return request({
    url: '/stock/gold_earnings/' + id,
    method: 'delete'
  })
}
