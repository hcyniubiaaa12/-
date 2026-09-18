<template>
  <div class="patient-root p-login">
    <div class="p-login__box">
      <div class="p-eyebrow" style="text-align: center">智 能 导 诊 系 统</div>
      <h1 class="p-login__title">分诊台</h1>
      <p class="p-login__hint">登录后即可开始症状分诊 · 管理员进入控制台</p>

      <div class="p-login__field">
        <label class="p-eyebrow" for="u">用 户 名</label>
        <input id="u" v-model="form.username" class="p-login__input" placeholder="请输入用户名" />
      </div>
      <div class="p-login__field">
        <label class="p-eyebrow" for="p">密 码</label>
        <input
          id="p"
          v-model="form.password"
          class="p-login__input"
          type="password"
          placeholder="请输入密码"
          @keydown.enter="submit"
        />
      </div>

      <p v-if="error" class="p-login__err">{{ error }}</p>

      <button class="p-btn" style="margin-top: 18px" @click="submit">登 录</button>

      <p class="p-login__tip">演示：admin / 123456 进管理端，其他用户名进患者端</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import '../../styles/patient.css'

const router = useRouter()
const route = useRoute()
const user = useUserStore()

const form = ref({ username: '', password: '' })
const error = ref('')

// 假数据登录：真实实现 POST /api/auth/login → { token, role, nickname }
function submit() {
  if (!form.value.username || !form.value.password) {
    error.value = '请填写用户名与密码'
    return
  }
  const isAdmin = form.value.username === 'admin'
  user.setLogin({
    token: 'mock-token',
    role: isAdmin ? 'admin' : 'patient',
    nickname: isAdmin ? '系统管理员' : form.value.username
  })
  router.push(route.query.redirect || (isAdmin ? '/admin/dashboard' : '/'))
}
</script>

<style scoped>
.p-login {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.p-login__box {
  width: 100%;
  max-width: 360px;
  background: var(--card);
  border: 1px solid var(--line);
  padding: 28px 22px 22px;
}
.p-login__title {
  margin: 8px 0 4px;
  text-align: center;
  font-size: 26px;
  font-weight: 400;
  letter-spacing: .12em;
}
.p-login__hint {
  margin-bottom: 22px;
  text-align: center;
  font-size: 12.5px;
  color: var(--ink-2);
}
.p-login__field { margin-bottom: 14px; }
.p-login__field label { display: block; margin-bottom: 5px; }
.p-login__input {
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 0;
  background: var(--card);
  font-family: var(--serif);
  font-size: 14px;
  color: var(--ink);
}
.p-login__input:focus { outline: none; border-color: var(--teal); }
.p-login__err { font-size: 12.5px; color: var(--err); }
.p-login__tip {
  margin-top: 14px;
  text-align: center;
  font-size: 11.5px;
  color: var(--ink-2);
}
</style>
