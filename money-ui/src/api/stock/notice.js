import request from '@/utils/request'

// 查询证券上市通知列表
export function listNotice(query) {
  return request({
    url: '/stock/notice/list',
    method: 'get',
    params: query
  })
}

// 查询证券上市通知详细
export function getNotice(id) {
  return request({
    url: '/stock/notice/' + id,
    method: 'get'
  })
}

// 新增证券上市通知
export function addNotice(data) {
  return request({
    url: '/stock/notice',
    method: 'post',
    data: data
  })
}

// 修改证券上市通知
export function updateNotice(data) {
  return request({
    url: '/stock/notice',
    method: 'put',
    data: data
  })
}

// 删除证券上市通知
export function delNotice(id) {
  return request({
    url: '/stock/notice/' + id,
    method: 'delete'
  })
}
