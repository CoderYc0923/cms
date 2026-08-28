import { Boot } from '@wangeditor/editor'
import { editImageSizeMenuConf } from './menus/editImageSize'
import { deleteVideoMenuConf } from './menus/deleteVideo'

let registered = false

export function registerEditorMenus () {
  if (registered) {
    return
  }

  Boot.registerModule({
    menus: [editImageSizeMenuConf, deleteVideoMenuConf]
  })

  registered = true
}
