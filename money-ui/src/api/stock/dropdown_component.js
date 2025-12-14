import request from '@/utils/request'

// 查询用户列表
export function listUser(query) {
  return request({
    url: '/stock/dropdownComponentCont/listUser',
    method: 'get',
    params: query
  })
}
