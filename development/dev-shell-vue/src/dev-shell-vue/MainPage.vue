<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

interface User {
  id: string
  email?: string
  firstName?: string
  lastName?: string
  groups?: string[]
  attributes?: Record<string, string[]> | null
}

const props = defineProps<{
  additionalRoutes?: string[]
}>()

const router = useRouter()
const users = ref<User[]>([])
const currentUser = ref<string | undefined>(undefined)
const baseUrl = '/dev-shell'

onMounted(async () => {
  try {
    const allUsersResp = await fetch(`${baseUrl}/user/all`)
    users.value = await allUsersResp.json()

    const currentUserResp = await fetch(`${baseUrl}/user/`, {
      headers: { Accept: 'text/plain' },
      credentials: 'include',
    })
    currentUser.value = await currentUserResp.text()
  } catch {
    currentUser.value = undefined
  }
})

async function changeUser(userId: string) {
  try {
    await fetch(`${baseUrl}/user/${userId}`, {
      method: 'POST',
      credentials: 'include',
    })
    window.location.reload()
  } catch (error) {
    console.error('Error changing user:', error)
  }
}

function navigateTo(target: string) {
  router.push(`/${target}`)
}
</script>

<template>
  <div class="dev-shell main-page">
    <h1>VanillaBP Business Cockpit Dev Shell</h1>

    <div class="user-selector">
      <span>User:</span>
      <select
        :value="currentUser ?? ''"
        @change="(e) => {
          const val = (e.target as HTMLSelectElement).value
          currentUser = val
          changeUser(val)
        }"
      >
        <option value="---">---</option>
        <option v-for="user in users" :key="user.id" :value="user.id">
          {{ user.id }}, {{ user.firstName }} {{ user.lastName }}
        </option>
      </select>
    </div>

    <div class="component-buttons">
      <button @click="navigateTo('task')">task</button>
      <button @click="navigateTo('workflow')">workflow</button>
    </div>

    <template v-if="additionalRoutes && additionalRoutes.length > 0">
      <div>Custom components:</div>
      <div class="component-buttons">
        <button
          v-for="route in additionalRoutes"
          :key="route"
          @click="navigateTo(route)"
        >
          {{ route }}
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dev-shell {
  margin: 2rem;
  font-family: 'Arial', sans-serif;
}

h1 {
  font-weight: bold;
  margin-bottom: 1.5rem;
  color: #613500;
}

.component-buttons {
  display: flex;
  gap: 1rem;
  flex-direction: column;
  margin: 1rem 0;
  width: 100px;
}

.component-buttons button {
  padding: 0.5rem 1rem;
  background-color: #b88d00;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.component-buttons button:hover {
  background-color: #fbe495;
  color: #613500;
}

.user-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1.5rem;
}

.user-selector span {
  color: #613500;
}

.user-selector select {
  padding: 0.5rem;
  border: 1px solid #b88d00;
  border-radius: 4px;
  font-size: 1rem;
  min-width: 250px;
  color: #613500;
  background-color: white;
  appearance: none;
  padding-right: 1.5rem;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23613500' width='18px' height='18px'%3e%3cpath d='M7 10l5 5 5-5z'/%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 0.5rem center;
}

.user-selector select:focus {
  outline: none;
  border-color: #613500;
  box-shadow: 0 0 0 2px rgba(184, 141, 0, 0.2);
}
</style>
