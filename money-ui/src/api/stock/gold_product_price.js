import request from '@/utils/request'

// 查询黄金价格列表
export function listGold_product_price(query) {
  return request({
    url: '/stock/gold_product_price/list',
    method: 'get',
    params: query
  })
}

// 查询黄金价格详细
export function getGold_product_price(id) {
  return request({
    url: '/stock/gold_product_price/' + id,
    method: 'get'
  })
}

// 新增黄金价格
export function addGold_product_price(data) {
  return request({
    url: '/stock/gold_product_price',
    method: 'post',
    data: data
  })
}

// 修改黄金价格
export function updateGold_product_price(data) {
  return request({
    url: '/stock/gold_product_price',
    method: 'put',
    data: data
  })
}

// 删除黄金价格
export function delGold_product_price(id) {
  return request({
    url: '/stock/gold_product_price/' + id,
    method: 'delete'
  })
}
