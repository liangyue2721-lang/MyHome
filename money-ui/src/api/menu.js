import request from '@/utils/request'

// 获取菜单路由
export function getRouters() {
  // 模拟后台返回的菜单路由数据，实际应该由后端接口返回
  const routers = [
    {
      name: 'Monitor',
      path: '/monitor',
      component: 'Layout',
      meta: { title: '系统监控', icon: 'monitor' },
      children: [
        {
          name: 'Server',
          path: 'server',
          component: 'monitor/server/index',
          meta: { title: '服务器监控', icon: 'server', permissions: ['monitor:server:list'] }
        },
        {
          name: 'ThreadPool',
          path: 'threadPool',
          component: 'monitor/threadPool/index',
          meta: { title: '线程池监控', icon: 'el-icon-monitor', permissions: ['monitor:threadPool:list'] }
        }
      ]
    },
    {
      name: 'System',
      path: '/system',
      component: 'Layout',
      meta: { title: '系统管理', icon: 'system' },
      children: [
        {
          name: 'User',
          path: 'user',
          component: 'system/user/index',
          meta: { title: '用户管理', icon: 'user', permissions: ['system:user:list'] }
        },
        {
          name: 'Role',
          path: 'role',
          component: 'system/role/index',
          meta: { title: '角色管理', icon: 'role', permissions: ['system:role:list'] }
        },
        {
          name: 'Menu',
          path: 'menu',
          component: 'system/menu/index',
          meta: { title: '菜单管理', icon: 'menu', permissions: ['system:menu:list'] }
        },
        {
          name: 'Dept',
          path: 'dept',
          component: 'system/dept/index',
          meta: { title: '部门管理', icon: 'dept', permissions: ['system:dept:list'] }
        },
        {
          name: 'Post',
          path: 'post',
          component: 'system/post/index',
          meta: { title: '岗位管理', icon: 'post', permissions: ['system:post:list'] }
        },
        {
          name: 'Dict',
          path: 'dict',
          component: 'system/dict/index',
          meta: { title: '字典管理', icon: 'dict', permissions: ['system:dict:list'] }
        },
        {
          name: 'Config',
          path: 'config',
          component: 'system/config/index',
          meta: { title: '参数设置', icon: 'config', permissions: ['system:config:list'] }
        },
        {
          name: 'Notice',
          path: 'notice',
          component: 'system/notice/index',
          meta: { title: '通知公告', icon: 'notice', permissions: ['system:notice:list'] }
        },
        {
          name: 'Log',
          path: 'log',
          component: 'Layout',
          meta: { title: '日志管理', icon: 'log' },
          children: [
            {
              name: 'OperLog',
              path: 'operlog',
              component: 'system/log/operlog/index',
              meta: { title: '操作日志', icon: 'form', permissions: ['system:operlog:list'] }
            },
            {
              name: 'LoginLog',
              path: 'loginlog',
              component: 'system/log/loginlog/index',
              meta: { title: '登录日志', icon: 'logininfor', permissions: ['system:logininfor:list'] }
            }
          ]
        }
      ]
    },
    {
      name: 'Project',
      path: '/project',
      component: 'Layout',
      meta: { title: '项目管理', icon: 'project' },
      children: [
        {
          name: 'Project',
          path: 'project',
          component: 'project/project/index',
          meta: { title: '项目管理', icon: 'project', permissions: ['project:project:list'] }
        }
      ]
    },
    {
      name: 'Tool',
      path: '/tool',
      component: 'Layout',
      meta: { title: '系统工具', icon: 'tool' },
      children: [
        {
          name: 'Build',
          path: 'build',
          component: 'tool/build/index',
          meta: { title: '构建管理', icon: 'build', permissions: ['tool:build:list'] }
        },
        {
          name: 'Gen',
          path: 'gen',
          component: 'tool/gen/index',
          meta: { title: '代码生成', icon: 'code', permissions: ['tool:gen:list'] }
        },
        {
          name: 'Swagger',
          path: 'swagger',
          component: 'tool/swagger/index',
          meta: { title: '系统接口', icon: 'swagger', permissions: ['tool:swagger:list'] }
        }
      ]
    },
    {
      name: 'Admin',
      path: '/admin',
      component: 'Layout',
      meta: { title: '系统管理', icon: 'admin' },
      children: [
        {
          name: 'Admin',
          path: 'admin',
          component: 'admin/index',
          meta: { title: '系统管理', icon: 'admin', permissions: ['admin:admin:list'] }
        }
      ]
    }
  ]
  return request({
    url: '/getRouters',
    method: 'get',
    data: routers
  })
}
