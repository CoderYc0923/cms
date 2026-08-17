/*
 * 获取cookie中的唯一标志
 * */

import Cookies from 'js-cookie'
const __TOKEN_STORAGE_KEY__ = 'BACK_USERID'
const __SESSION_STORAGE_KEY__ = 'session_id'

export function getToken () {
  return Cookies.get(__TOKEN_STORAGE_KEY__)
}

export function setToken (token) {
  Cookies.set(__TOKEN_STORAGE_KEY__, token)
}

export function clearToken () {
  Cookies.remove(__TOKEN_STORAGE_KEY__)
}

export function getSession () {
  return Cookies.get(__SESSION_STORAGE_KEY__)
}

export function setSession (session) {
  Cookies.set(__SESSION_STORAGE_KEY__, session)
}

export function clearSession () {
  Cookies.remove(__SESSION_STORAGE_KEY__)
}
