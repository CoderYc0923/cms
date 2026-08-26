import { Boot } from '@wangeditor/editor'
import { editImageSizeMenuConf } from './menus/editImageSize'

let registered = false

export function registerEditorMenus () {
  if (registered) {
    return
  }

  Boot.registerModule({
    menus: [editImageSizeMenuConf]
  })

  registered = true
}
