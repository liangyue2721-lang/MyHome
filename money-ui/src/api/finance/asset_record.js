import request from '@/utils/request'

// 查询个人资产明细列表
export function listAssetRecord(query) {
  return request({
    url: '/finance/asset_record/list',
    method: 'get',
    params: query
  })
}

// 查询个人资产明细详细
export function getAssetRecord(assetId) {
  return request({
    url: '/finance/asset_record/' + assetId,
    method: 'get'
  })
}

// 新增个人资产明细
export function addAssetRecord(data) {
  return request({
    url: '/finance/asset_record',
    method: 'post',
    data: data
  })
}

// 修改个人资产明细
export function updateAssetRecord(data) {
  return request({
    url: '/finance/asset_record',
    method: 'put',
    data: data
  })
}

// 删除个人资产明细
export function delAssetRecord(assetId) {
  return request({
    url: '/finance/asset_record/' + assetId,
    method: 'delete'
  })
}


// 获取资产类型
export function getAssetType() {
  return request({
    url: '/finance/asset_record/getAssetType',
    method: 'get'
  })
}

// 获取资产状态
export function getAssetStatus() {
  return request({
    url: '/finance/asset_record/getAssetStatus',
    method: 'get'
  })
}

// 获取扇形图数据
export function getAssetRecordColumnChart() {
  return request({
    url: '/finance/asset_record/getRecordColumnChart',
    method: 'get'
  })
}

export function getSync() {
  return request({
    url: '/finance/asset_record/sync',
    method: 'get'
  })
}
