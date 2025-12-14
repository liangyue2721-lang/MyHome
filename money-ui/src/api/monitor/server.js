import request from '@/utils/request'

// 获取服务器信息
export function getServer() {
  return request({
    url: '/monitor/server',
    method: 'get'
  })
}

// 获取集群线程池信息（内存模式）
export function getClusterThreadPool() {
  return request({
    url: '/monitor/server/clusterThreadPool',
    method: 'get'
    })
}

// 获取集群线程池信息（Redis模式）
export function getClusterThreadPoolRedis() {
  return request({
    url: '/monitor/server/clusterThreadPoolRedis',
    method: 'get'
    })
}

// 获取集群线程池聚合统计信息（Redis模式）
export function getAggregatedThreadPoolRedis() {
  return request({
    url: '/monitor/server/aggregatedThreadPoolRedis',
    method: 'get'
    })
}

// 获取集群服务器信息（Redis模式）
export function getClusterServerRedis() {
  return request({
    url: '/monitor/server/clusterServerRedis',
    method: 'get'
    })
}

// 获取网络流量信息
export function getNetworkTraffic() {
  return request({
    url: '/monitor/server/networkTraffic',
    method: 'get'
  })
}