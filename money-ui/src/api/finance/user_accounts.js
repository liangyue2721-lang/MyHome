import request from '@/utils/request'

// 查询用户账户银行卡信息列表
export function listUser_accounts(query) {
  return request({
    url: '/finance/user_accounts/list',
    method: 'get',
    params: query
  })
}

// 查询用户账户银行卡信息详细
export function getUser_accounts(id) {
  return request({
    url: '/finance/user_accounts/' + id,
    method: 'get'
  })
}

// 新增用户账户银行卡信息
export function addUser_accounts(data) {
  return request({
    url: '/finance/user_accounts',
    method: 'post',
    data: data
  })
}

// 修改用户账户银行卡信息
export function updateUser_accounts(data) {
  return request({
    url: '/finance/user_accounts',
    method: 'put',
    data: data
  })
}

// 删除用户账户银行卡信息
export function delUser_accounts(id) {
  return request({
    url: '/finance/user_accounts/' + id,
    method: 'delete'
  })
}
