import { auth } from './auth'

export const directives = app => {
  app.directive('auth', auth)
}
