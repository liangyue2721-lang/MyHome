import request from '@/utils/request'

// 获取线程池信息
export function getThreadPool() {
  return request({
    url: '/monitor/threadPool',
    method: 'get'
  })
}