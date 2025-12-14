import request from '@/utils/request'

// 查询买房支出记录列表
export function listHouseExpense(query) {
  return request({
    url: '/finance/house_expense/list',
    method: 'get',
    params: query
  })
}

// 查询买房支出记录详细
export function getHouseExpense(id) {
  return request({
    url: '/finance/house_expense/' + id,
    method: 'get'
  })
}

// 新增买房支出记录
export function addHouseExpense(data) {
  return request({
    url: '/finance/house_expense',
    method: 'post',
    data: data
  })
}

// 修改买房支出记录
export function updateHouseExpense(data) {
  return request({
    url: '/finance/house_expense',
    method: 'put',
    data: data
  })
}

// 删除买房支出记录
export function delHouse_expense(id) {
  return request({
    url: '/finance/house_expense/' + id,
    method: 'delete'
  })
}
