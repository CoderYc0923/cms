/*
 * Token 存储（access + refresh）
 */

import Cookies from 'js-cookie'

const ACCESS_TOKEN_KEY = 'CMS_ACCESS_TOKEN'
const REFRESH_TOKEN_KEY = 'CMS_REFRESH_TOKEN'
/** 兼容旧 cookie 名，清登录态时一并删掉 */
const LEGACY_TOKEN_KEY = 'BACK_USERID'
const SESSION_STORAGE_KEY = 'session_id'

export function getToken () {
  return Cookies.get(ACCESS_TOKEN_KEY) || Cookies.get(LEGACY_TOKEN_KEY)
}

export function setToken (token) {
  Cookies.set(ACCESS_TOKEN_KEY, token)
}

export function clearToken () {
  Cookies.remove(ACCESS_TOKEN_KEY)
  Cookies.remove(LEGACY_TOKEN_KEY)
}

export function getRefreshToken () {
  return Cookies.get(REFRESH_TOKEN_KEY)
}

export function setRefreshToken (token) {
  Cookies.set(REFRESH_TOKEN_KEY, token)
}

export function clearRefreshToken () {
  Cookies.remove(REFRESH_TOKEN_KEY)
}

export function getSession () {
  return Cookies.get(SESSION_STORAGE_KEY)
}

export function setSession (session) {
  Cookies.set(SESSION_STORAGE_KEY, session)
}

export function clearSession () {
  Cookies.remove(SESSION_STORAGE_KEY)
}
