<template>
  <div class="login">
    <div class="login-overlay"></div>

    <div class="login-form-container">
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <div class="title-container">
          <h3 class="title">{{ title }}</h3>
        </div>

        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            placeholder="帳號"
            class="custom-input"
          >
            <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon"/>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="密碼"
            @keyup.enter.native="handleLogin"
            class="custom-input"
          >
            <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon"/>
          </el-input>
        </el-form-item>

        <el-form-item prop="code" v-if="captchaEnabled">
          <div class="code-wrap">
            <el-input
              v-model="loginForm.code"
              auto-complete="off"
              placeholder="驗證碼"
              class="custom-input code-input"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon"/>
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img" title="點擊切換驗證碼"/>
            </div>
          </div>
        </el-form-item>

        <div class="extra-options">
          <el-checkbox v-model="loginForm.rememberMe">記住密碼</el-checkbox>
          <div v-if="register">
            <router-link class="link-type" :to="'/register'">立即註冊</router-link>
          </div>
        </div>

        <el-form-item style="width:100%;">
          <el-button
            :loading="loading"
            size="medium"
            type="primary"
            class="login-btn"
            @click.native.prevent="handleLogin"
          >
            <span v-if="!loading">登 錄</span>
            <span v-else>登 錄 中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="el-login-footer">
      <span>Copyright © 2018-2025 nadoutong.vip All Rights Reserved.</span>
    </div>
  </div>
</template>

<script>
import {getCodeImg} from "@/api/login"
import Cookies from "js-cookie"
import {encrypt, decrypt} from '@/utils/jsencrypt'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE || "系統登錄",
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          {required: true, trigger: "blur", message: "請輸入您的帳號"}
        ],
        password: [
          {required: true, trigger: "blur", message: "請輸入您的密碼"}
        ],
        code: [{required: true, trigger: "change", message: "請輸入驗證碼"}]
      },
      loading: false,
      captchaEnabled: true,
      register: false,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function (route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          Cookies.set("username", this.loginForm.username, {expires: 30, path: '/'})
          if (this.loginForm.rememberMe) {
            Cookies.set("password", encrypt(this.loginForm.password), {expires: 30, path: '/'})
            Cookies.set('rememberMe', this.loginForm.rememberMe, {expires: 30, path: '/'})
          } else {
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({path: this.redirect || "/"}).catch(() => {
            })
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
@import "@/assets/styles/global.scss";
/* === 修改點 1：輸入框樣式調整 ===
   為了在透明背景下看清輸入框，這裡將輸入框背景設為半透明白
*/
::v-deep .el-input__inner {
  height: 40px;
  /* 輸入框背景：半透明白，保證文字可讀性 */
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: #fff; /* 輸入文字顏色設為白色（如果背景深）或黑色 */
  padding-left: 35px;
  transition: all 0.3s;

  &::placeholder {
    color: rgba(255, 255, 255, 0.7); /* 提示文字顏色 */
  }

  &:focus {
    border-color: #409EFF;
    background: rgba(255, 255, 255, 0.9); /* 聚焦時變亮 */
    color: #333;

    &::placeholder {
      color: #999;
    }
  }
}

::v-deep .el-input__prefix {
  left: 8px;
  color: #eee; /* 圖標顏色改為淺色 */
}

::v-deep .el-checkbox__label {
  color: #fff; /* 複選框文字改為白色 */
}

::v-deep .el-checkbox__input.is-checked + .el-checkbox__label {
  color: #409EFF;
}

.login {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  overflow: hidden;
}

/* 背景遮罩：稍微加深背景，讓白色文字和透明框更清晰 */
.login-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  z-index: 0;
}

.login-form-container {
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s ease-out forwards;
}

/* === 修改點 2：透明化登錄框 ===
*/
.login-form {
  border-radius: 12px;

  /* 方案 A: 全透明（僅有邊框感）
     background: rgba(0, 0, 0, 0.2);
     border: 1px solid rgba(255, 255, 255, 0.3);
  */

  /* 方案 B: 高通透磨砂感（推薦，更精緻） */
  background: rgba(255, 255, 255, 0.15); /* 極低透明度的白色 */
  backdrop-filter: blur(5px); /* 輕微模糊背景 */
  -webkit-backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.2); /* 淡淡的白邊框 */
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.2);

  width: 400px;
  padding: 30px 35px;

  .title-container {
    text-align: center;
    margin-bottom: 30px;

    .title {
      font-size: 26px;
      /* 標題顏色改為白色，帶一點陰影防止背景太花看不清 */
      color: #fff;
      margin: 0;
      font-weight: 600;
      letter-spacing: 1px;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
    }
  }

  .input-icon {
    height: 16px;
    width: 16px;
    vertical-align: middle;
    margin-top: 12px;
  }
}

.code-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .code-input {
    flex: 1;
    margin-right: 15px;
  }

  .login-code {
    width: 110px;
    height: 40px;
    border-radius: 4px;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.5); /* 邊框適配透明風格 */
    cursor: pointer;
    background: #fff; /* 驗證碼背景保持白色 */

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.extra-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;

  .link-type {
    color: #fff; /* 文字改白 */
    font-size: 14px;
    text-decoration: none;
    opacity: 0.8;

    &:hover {
      text-decoration: underline;
      color: #409EFF;
      opacity: 1;
    }
  }
}

.login-btn {
  width: 100%;
  padding: 12px 0;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 6px;
  /* 按鈕改為稍微半透明的漸變，或者保持實色以突出 */
  background: linear-gradient(90deg, #1890ff, #409EFF);
  border: none;
  box-shadow: 0 4px 14px 0 rgba(0, 0, 0, 0.3);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px 0 rgba(64, 158, 255, 0.6);
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: absolute;
  bottom: 10px;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

@keyframes slideUp {
  0% {
    opacity: 0;
    transform: translateY(30px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .login-form {
    width: 85%;
    padding: 25px 20px;
    margin: 0 auto;
    background: rgba(0, 0, 0, 0.5); /* 手機端可能需要深色背景保證可讀性 */

    .title-container .title {
      font-size: 22px;
    }
  }
}
</style>
