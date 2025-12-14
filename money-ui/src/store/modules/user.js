import router from '@/router'
import {MessageBox} from 'element-ui'
import {login, logout, getInfo} from '@/api/login'
import {getToken, setToken, removeToken} from '@/utils/auth'
import {isHttp, isEmpty} from '@/utils/validate'
import {GetLicenseCheck} from '@/api/finance/pieChart'
import defAva from '@/assets/images/profile.jpg'

const user = {
  state: {
    token: getToken(),
    id: '',
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: []
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_ID: (state, id) => {
      state.id = id
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_NICK_NAME: (state, nickName) => {
      state.nickName = nickName
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
    }
  },

  actions: {
    // 登录
    Login({commit}, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid)
          .then(res => {
            setToken(res.token)
            commit('SET_TOKEN', res.token)
            resolve()
          })
          .catch(error => reject(error))
      })
    },

    // ===============================
    // 优化后的 GetInfo 方法（兼容数组格式返回）
    // ===============================
    GetInfo({commit, state}) {
      return new Promise((resolve, reject) => {
        getInfo()
          .then(async res => {
            const user = res.user
            let avatar = user.avatar || ''
            if (!isHttp(avatar)) {
              avatar = isEmpty(avatar)
                ? defAva
                : process.env.VUE_APP_BASE_API + avatar
            }

            if (res.roles && res.roles.length > 0) {
              commit('SET_ROLES', res.roles)
              commit('SET_PERMISSIONS', res.permissions)
            } else {
              commit('SET_ROLES', ['ROLE_DEFAULT'])
            }

            commit('SET_ID', user.userId)
            commit('SET_NAME', user.userName)
            commit('SET_NICK_NAME', user.nickName)
            commit('SET_AVATAR', avatar)

            // 初始密码提示
            if (res.isDefaultModifyPwd) {
              MessageBox.confirm('您的密码还是初始密码，请修改密码！', '安全提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
              }).then(() => {
                router.push({name: 'Profile', params: {activeTab: 'resetPwd'}})
              }).catch(() => {
              })
            }

            // 密码过期提示
            if (!res.isDefaultModifyPwd && res.isPasswordExpired) {
              MessageBox.confirm('您的密码已过期，请尽快修改密码！', '安全提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
              }).then(() => {
                router.push({name: 'Profile', params: {activeTab: 'resetPwd'}})
              }).catch(() => {
              })
            }

            // ===============================
            // 新增：服务器许可证信息提示（兼容数组格式）
            // ===============================
            try {
              const licenseData = await GetLicenseCheck()
              const licenseList = Array.isArray(licenseData) ? licenseData : [licenseData]

              let msgList = ''
              licenseList.forEach((lic, index) => {
                const {
                  serverName,
                  ipAddress,
                  purchaseDate,
                  expireDate,
                  remindDays,
                  remainingDays
                } = lic

                // 修正：使用正确的 expireDate 变量名
                const daysLeft =
                  remainingDays !== undefined
                    ? remainingDays
                    : Math.ceil(
                      (new Date(expireDate) - new Date()) / (1000 * 60 * 60 * 24)
                    )

                const isExpiringSoon = daysLeft <= remindDays

                msgList += `
      <div style="margin-bottom:10px; line-height:1.6;">
        <b>服务器 ${index + 1}</b><br>
        服务器名称：${serverName}<br>
        IP 地址：${ipAddress}<br>
        服务提供商：${lic.provider || '未知'}<br>
        购买日期：${purchaseDate || '未知'}<br>
        到期日期：${expireDate}<br>
        距离到期：
        <span style="color:${isExpiringSoon ? 'red' : 'green'};font-weight:bold;">
          ${daysLeft} 天
        </span>
      </div>
      <hr style="margin:6px 0; border:none; border-top:1px dashed #ccc;">
    `
              })

              MessageBox.alert(msgList, '服务器预警信息', {
                confirmButtonText: '确定',
                type: 'success',
                dangerouslyUseHTMLString: true
              }).catch(() => {
              })
            } catch (e) {
              MessageBox.alert('欢迎登录系统，祝您使用愉快！', '登录成功', {
                confirmButtonText: '确定',
                type: 'success'
              }).catch(() => {
              })
            }


            resolve(res)
          })
          .catch(error => reject(error))
      })
    },

    // 退出系统
    LogOut({commit, state}) {
      return new Promise((resolve, reject) => {
        logout(state.token)
          .then(() => {
            commit('SET_TOKEN', '')
            commit('SET_ROLES', [])
            commit('SET_PERMISSIONS', [])
            removeToken()
            resolve()
          })
          .catch(error => reject(error))
      })
    },

    // 前端登出
    FedLogOut({commit}) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        removeToken()
        resolve()
      })
    }
  }
}

export default user
