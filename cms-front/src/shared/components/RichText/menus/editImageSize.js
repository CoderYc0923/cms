import { DomEditor, SlateTransforms } from '@wangeditor/editor'

const MODAL_ID = 'cms-edit-image-size'

function normalizeSize (value) {
  const raw = String(value || '').trim()
  if (!raw) {
    return ''
  }
  if (raw.endsWith('%') || raw.endsWith('px')) {
    return raw
  }
  if (/^\d+(\.\d+)?$/.test(raw)) {
    return `${raw}px`
  }
  return raw
}

function getSelectedImageNode (editor) {
  return DomEditor.getSelectedNodeByType(editor, 'image')
}

class EditImageSizeMenu {
  constructor () {
    this.title = '尺寸'
    this.tag = 'button'
    this.showModal = true
    this.modalWidth = 320
    this.$content = null
  }

  getValue () {
    return ''
  }

  isActive () {
    return false
  }

  isDisabled (editor) {
    if (editor.selection == null) {
      return true
    }
    return getSelectedImageNode(editor) == null
  }

  exec () {}

  getModalPositionNode (editor) {
    return getSelectedImageNode(editor)
  }

  getModalContentElem (editor) {
    this.currentEditor = editor

    if (!this.$content) {
      const content = document.createElement('div')
      content.className = 'cms-image-size-modal'
      content.innerHTML = `
        <form class="cms-image-size-form">
          <label class="cms-image-size-field">
            <span class="cms-image-size-label">宽度</span>
            <input id="${MODAL_ID}-width" class="cms-image-size-input" placeholder="如 80% 或 400px" />
          </label>
          <label class="cms-image-size-field">
            <span class="cms-image-size-label">高度</span>
            <input id="${MODAL_ID}-height" class="cms-image-size-input" placeholder="留空为自动" />
          </label>
          <p class="cms-image-size-hint">支持百分比或像素，仅填数字默认 px</p>
        </form>
        <button type="button" id="${MODAL_ID}-ok" class="cms-image-size-ok">确定</button>
      `

      content.addEventListener('click', event => {
        const target = event.target
        if (!(target instanceof HTMLElement) || target.id !== `${MODAL_ID}-ok`) {
          return
        }
        event.preventDefault()

        const activeEditor = this.currentEditor
        if (!activeEditor) {
          return
        }

        const widthInput = content.querySelector(`#${MODAL_ID}-width`)
        const heightInput = content.querySelector(`#${MODAL_ID}-height`)
        const width = normalizeSize(widthInput?.value)
        const height = normalizeSize(heightInput?.value)

        activeEditor.restoreSelection()

        const imageNode = getSelectedImageNode(activeEditor)
        if (!imageNode) {
          return
        }

        const style = { ...(imageNode.style || {}) }
        if (width) {
          style.width = width
        } else {
          delete style.width
        }
        if (height) {
          style.height = height
        } else {
          delete style.height
        }

        SlateTransforms.setNodes(
          activeEditor,
          { style },
          { match: n => DomEditor.checkNodeType(n, 'image') }
        )

        activeEditor.hidePanelOrModal()
      })

      this.$content = content
    }

    const content = this.$content
    const widthInput = content.querySelector(`#${MODAL_ID}-width`)
    const heightInput = content.querySelector(`#${MODAL_ID}-height`)
    const imageNode = getSelectedImageNode(editor)

    if (imageNode && widthInput && heightInput) {
      const { style = {} } = imageNode
      widthInput.value = style.width || ''
      heightInput.value = style.height || ''
      setTimeout(() => widthInput.focus(), 0)
    }

    return content
  }
}

export const editImageSizeMenuConf = {
  key: 'editImageSize',
  factory () {
    return new EditImageSizeMenu()
  }
}
