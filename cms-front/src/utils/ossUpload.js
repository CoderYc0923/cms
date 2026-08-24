import { DEFAULT_BIZ_TYPE } from '@/consts/space'
import { resolveSpaceId } from '@/utils/space'
import {
  abortUpload,
  completeUpload,
  initUpload,
  signParts
} from '@/service/upload'

const SIGN_BATCH_SIZE = 10
const UPLOAD_MODE = {
  SINGLE: 'SINGLE',
  MULTIPART: 'MULTIPART'
}

export class UploadAbortError extends Error {
  constructor (message = '上传已取消') {
    super(message)
    this.name = 'UploadAbortError'
  }
}

function assertApiSuccess (res, fallbackMessage) {
  if (res?.code === 0 || res?.code === 200) {
    return res.data
  }
  throw new Error(res?.message || fallbackMessage)
}

function normalizeEtag (etag) {
  if (!etag) {
    throw new Error('OSS 未返回 ETag，请检查桶 CORS 是否暴露 ETag 响应头')
  }
  return etag
}

function createProgressReporter (onProgress) {
  return (percent) => {
    if (typeof onProgress === 'function') {
      onProgress(Math.min(100, Math.max(0, Math.round(percent))))
    }
  }
}

function throwIfAborted (signal) {
  if (signal?.aborted) {
    throw new UploadAbortError()
  }
}

function putToOss (url, body, { headers = {}, onPartProgress, signal, xhrRegistry } = {}) {
  return new Promise((resolve, reject) => {
    throwIfAborted(signal)

    const xhr = new XMLHttpRequest()
    xhr.open('PUT', url)
    xhrRegistry?.add(xhr)

    const cleanup = () => {
      xhrRegistry?.delete(xhr)
    }

    const rejectAbort = () => {
      cleanup()
      reject(new UploadAbortError())
    }

    const onSignalAbort = () => {
      xhr.abort()
    }

    signal?.addEventListener('abort', onSignalAbort)

    Object.entries(headers).forEach(([key, value]) => {
      if (value != null) {
        xhr.setRequestHeader(key, value)
      }
    })

    if (typeof onPartProgress === 'function') {
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          onPartProgress(event.loaded, event.total)
        }
      }
    }

    // 上传完成
    xhr.onload = () => {
      // 移除 abort 事件监听
      signal?.removeEventListener('abort', onSignalAbort)
      cleanup()

      if (signal?.aborted) {
        rejectAbort()
        return
      }

      if (xhr.status >= 200 && xhr.status < 300) {
        resolve({
          headers: {
            get: (name) => xhr.getResponseHeader(name)
          }
        })
        return
      }

      reject(new Error(`OSS 上传失败 (${xhr.status})`))
    }

    // 上传失败
    xhr.onerror = () => {
      // 移除 abort 事件监听
      signal?.removeEventListener('abort', onSignalAbort)
      cleanup()
      reject(signal?.aborted ? new UploadAbortError() : new Error('OSS 上传失败'))
    }

    // 上传取消
    xhr.onabort = () => {
      // 移除 abort 事件监听
      signal?.removeEventListener('abort', onSignalAbort)
      cleanup()
      reject(new UploadAbortError())
    }

    xhr.send(body)
  })
}

async function uploadSingle (init, file, report, context) {
  const { signal, xhrRegistry } = context
  report(5)
  throwIfAborted(signal)

  const contentType = init.headers?.contentType || file.type
  await putToOss(init.putUrl, file, {
    headers: { 'Content-Type': contentType },
    signal,
    xhrRegistry,
    onPartProgress: (loaded, total) => {
      const ratio = total > 0 ? loaded / total : 0
      report(5 + ratio * 90)
    }
  })

  throwIfAborted(signal)
  report(96)

  const complete = assertApiSuccess(
    await completeUpload(init.fileId, {}),
    '完成上传失败'
  )
  report(100)
  return complete.stableUrl
}

async function uploadMultipart (init, file, report, context) {
  const { signal, xhrRegistry } = context
  const partSize = init.partSize
  const partCount = init.partCount
  const etags = []
  const partLoadedMap = new Map()

  const reportBytes = () => {
    let uploadedBytes = 0
    partLoadedMap.forEach((loaded) => {
      uploadedBytes += loaded
    })
    const ratio = file.size > 0 ? uploadedBytes / file.size : 0
    report(5 + ratio * 90)
  }

  for (let startPart = 1; startPart <= partCount; startPart += SIGN_BATCH_SIZE) {
    throwIfAborted(signal)

    const endPart = Math.min(startPart + SIGN_BATCH_SIZE - 1, partCount)
    const partNumbers = Array.from(
      { length: endPart - startPart + 1 },
      (_, index) => startPart + index
    )

    report(3)

    const signed = assertApiSuccess(
      await signParts(init.fileId, { partNumbers }),
      '签发分片失败'
    )

    throwIfAborted(signal)

    const batchResults = await Promise.all(
      (signed.parts || []).map(async (part) => {
        throwIfAborted(signal)

        const chunkStart = (part.partNumber - 1) * partSize
        const chunkEnd = Math.min(chunkStart + partSize, file.size)
        const blob = file.slice(chunkStart, chunkEnd)
        partLoadedMap.set(part.partNumber, 0)

        const response = await putToOss(part.putUrl, blob, {
          signal,
          xhrRegistry,
          onPartProgress: (loaded) => {
            partLoadedMap.set(part.partNumber, loaded)
            reportBytes()
          }
        })

        partLoadedMap.set(part.partNumber, blob.size)
        reportBytes()

        return {
          partNumber: part.partNumber,
          etag: normalizeEtag(response.headers.get('ETag'))
        }
      })
    )

    etags.push(...batchResults)
  }

  throwIfAborted(signal)
  etags.sort((a, b) => a.partNumber - b.partNumber)

  report(96)
  const complete = assertApiSuccess(
    await completeUpload(init.fileId, { parts: etags }),
    '完成分片上传失败'
  )
  report(100)
  return complete.stableUrl
}

async function runUploadToOss (file, options, controller, state) {
  const spaceId = options.spaceId ?? resolveSpaceId(options.spaceSlug)
  if (!spaceId) {
    throw new Error('无法确定 spaceId，请检查路由或 SPACE_SLUG_ID_MAP 配置')
  }

  if (!file?.type) {
    throw new Error('无法识别文件类型')
  }

  const report = createProgressReporter(options.onProgress)
  const signal = controller.signal
  const xhrRegistry = new Set()
  const context = { signal, xhrRegistry }

  try {
    report(1)

    const init = assertApiSuccess(
      await initUpload({
        fileName: file.name,
        contentType: file.type,
        sizeBytes: file.size,
        bizType: options.bizType || DEFAULT_BIZ_TYPE,
        spaceId
      }),
      '初始化上传失败'
    )

    state.fileId = init.fileId
    report(3)

    if (state.abortRequested || signal.aborted) {
      await abortUpload(state.fileId).catch(() => {})
      throw new UploadAbortError()
    }

    throwIfAborted(signal)

    if (init.mode === UPLOAD_MODE.MULTIPART) {
      return await uploadMultipart(init, file, report, context)
    }

    return await uploadSingle(init, file, report, context)
  } catch (error) {
    if (!state.userCancelled && state.fileId) {
      await abortUpload(state.fileId).catch(() => {})
    }
    throw error
  } finally {
    xhrRegistry.clear()
  }
}

/**
 * 浏览器直传 OSS（经后端 init / sign / complete）
 * @returns {{ promise: Promise<string>, cancel: () => Promise<void> }}
 */
export function uploadToOss (file, options = {}) {
  const controller = new AbortController()
  const state = {
    fileId: null,
    userCancelled: false,
    abortRequested: false
  }

  // 取消上传
  const cancel = async () => {
    if (state.abortRequested) {
      return
    }
    state.abortRequested = true
    state.userCancelled = true
    controller.abort()

    if (state.fileId) {
      await abortUpload(state.fileId).catch(() => {})
    }
  }

  const promise = runUploadToOss(file, options, controller, state)

  return { promise, cancel }
}
