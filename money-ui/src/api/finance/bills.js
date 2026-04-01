import request from '@/utils/request'

// 查询月度账单 (单JSON架构)列表
export function listBills(query) {
  return request({
    url: '/finance/bills/list',
    method: 'get',
    params: query
  })
}

// 查询月度账单 (单JSON架构)列表
export function getViewList(query) {
  return request({
    url: '/finance/bills/getViewList',
    method: 'get',
    params: query
  })
}

// 查询月度账单 (单JSON架构)详细
export function getBills(id) {
  return request({
    url: '/finance/bills/' + id,
    method: 'get'
  })
}

// 新增月度账单 (单JSON架构)
export function addBills(data) {
  return request({
    url: '/finance/bills',
    method: 'post',
    data: data
  })
}

// 修改月度账单 (单JSON架构)
export function updateBills(data) {
  return request({
    url: '/finance/bills',
    method: 'put',
    data: data
  })
}

// 删除月度账单 (单JSON架构)
export function delBills(id) {
  return request({
    url: '/finance/bills/' + id,
    method: 'delete'
  })
}
