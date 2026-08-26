import axios from "axios";
import { message } from "ant-design-vue";
import { API_PREFIX } from "@/consts/const.js";
import { getSession, getToken } from "./token";
import { idRegex, linkRegexp } from "./regexp";

/**
 * 处理 HTML 字符串，为标题添加 id，并生成嵌套目录树
 * @param {string} html 原始 HTML 字符串
 * @returns {object} { processedHtml, anchorItems }
 */
export function processHtmlForToc(html) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, 'text/html');

  const headings = doc.body.querySelectorAll('h1, h2, h3, h4, h5, h6');
  if (!headings.length) {
    return { processedHtml: html, anchorItems: [] };
  }

  const headingInfos = [];
  const idSet = new Set(); // 用于保证 id 唯一

  headings.forEach((heading, index) => {
    const rawText = heading.textContent?.trim() || `heading-${index}`;
    // 生成基础 slug（根据顺序和层级生成，确保唯一
    const baseSlug = `${index}-${heading.tagName.substring(1)}`;
    /* const baseSlug = rawText
      .replace(/[^\w\u4e00-\u9fa5]+/g, '-') // 将非单词字符（含中文）替换为连字符
      .replace(/^-+|-+$/g, '')               // 去掉首尾连字符
      .toLowerCase() || 'heading'; */

    // 确保 id 唯一：如果 baseSlug 已存在，则添加后缀
    let id = baseSlug;
    let counter = 1;
    while (idSet.has(id)) {
      id = `${baseSlug}-${counter}`;
      counter++;
    }
    idSet.add(id);

    // 给标题元素设置 id
    heading.id = id;

    // 记录标题信息（用于构建目录树）
    headingInfos.push({
      id,
      text: rawText,
      level: parseInt(heading.tagName.substring(1)), // h2 -> 2
    });
  });

  // 4. 构建嵌套目录树（适配 a-anchor 的 items）
  const buildTree = (infos) => {
    const tree = [];
    const stack = []; // 栈结构，用于处理嵌套关系

    infos.forEach((info) => {
      const node = {
        href: `#${info.id}`,
        title: info.text,
        children: [],
      };

      // 如果栈为空，说明是根节点
      if (stack.length === 0) {
        tree.push(node);
        stack.push({ level: info.level, node });
      } else {
        // 弹出所有级别大于等于当前级别的栈顶元素，直到找到父级
        while (stack.length > 0 && stack[stack.length - 1].level >= info.level) {
          stack.pop();
        }
        if (stack.length === 0) {
          // 没有父级，作为新的根节点
          tree.push(node);
        } else {
          // 作为栈顶节点的子节点
          stack[stack.length - 1].node.children.push(node);
        }
        // 当前节点入栈，供后续子节点使用
        stack.push({ level: info.level, node });
      }
    });
    return tree;
  };

  const anchorItems = buildTree(headingInfos);

  const processedHtml = doc.body.innerHTML;

  return { processedHtml, anchorItems };
}

/**
 * 触发 window.resize
 */
export function triggerWindowResizeEvent() {
  const event = document.createEvent("HTMLEvents");
  event.initEvent("resize", true, true);
  event.eventType = "message";
  window.dispatchEvent(event);
}

export function handleScrollHeader(callback) {
  let timer = 0;

  let beforeScrollTop = window.pageYOffset;
  callback = callback || function () { };
  window.addEventListener(
    "scroll",
    () => {
      clearTimeout(timer);
      timer = setTimeout(() => {
        let direction = "up";
        const afterScrollTop = window.pageYOffset;
        const delta = afterScrollTop - beforeScrollTop;
        if (delta === 0) {
          return false;
        }
        direction = delta > 0 ? "down" : "up";
        callback(direction);
        beforeScrollTop = afterScrollTop;
      }, 50);
    },
    false
  );
}

export function isIE() {
  const bw = window.navigator.userAgent;
  const compare = (s) => bw.indexOf(s) >= 0;
  const ie11 = (() => "ActiveXObject" in window)();
  return compare("MSIE") || ie11;
}

/**
 * Remove loading animate
 * @param id parent element id or class
 * @param timeout
 */
export function removeLoadingAnimate(id = "", timeout = 1500) {
  if (id === "") {
    return;
  }
  setTimeout(() => {
    document.body.removeChild(document.getElementById(id));
  }, timeout);
}

/*
 * 手机号加密
 * @param phone
 * */
export function secretPhone(phone = "") {
  if (typeof phone !== "string") {
    console.warn("phone is string");
    return phone;
  }
  const len = phone.length;
  return `${phone.slice(0, 3)}****${phone.slice(len - 4, len)}`;
}

/**
 * image转Blob
 */
export function imageToBlob(src, cb, onErr) {
  imageToCanvas(
    src,
    function (canvas) {
      cb(dataURLToBlob(canvasToDataURL(canvas, "", 0.8)));
    },
    onErr
  );
}

/**
 * image转canvas：图片地址
 */
export function imageToCanvas(src, cb, onErr) {
  const canvas = document.createElement("CANVAS");
  const ctx = canvas.getContext("2d");
  const img = new Image();
  img.setAttribute("crossOrigin", "Anonymous");
  img.src = src;
  img.onload = function () {
    canvas.width = img.width;
    canvas.height = img.height;
    ctx.drawImage(img, 0, 0);
    cb(canvas);
  };
  img.onerror = onErr;
}

/**
 * canvas转dataURL：canvas对象、转换格式、图像品质
 */
export function canvasToDataURL(canvas, format, quality) {
  return canvas.toDataURL(format || "image/jpeg", quality || 1.0);
}

/**
 * DataURL转Blob对象
 */
export function dataURLToBlob(dataurl) {
  const arr = dataurl.split(",");
  const mime = arr[0].match(/:(.*?);/)[1];
  const bstr = atob(arr[1]);
  let n = bstr.length;
  const u8arr = new Uint8Array(n);
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n);
  }
  return new Blob([u8arr], { type: mime });
}

export function getBase64(img, callback) {
  const reader = new FileReader();
  reader.addEventListener("load", () => callback(reader.result));
  reader.readAsDataURL(img);
}

/**
 * 混合分割, 接受多个分隔符
 * @param str {string} 待分割的字符串
 * @param splits {array} 分隔符数组, 默认分隔符为空格
 * @return {array} 分割后的字符串
 */
export function mixSplit(str, splits = [" "]) {
  const split = splits[0];
  for (let i = 0; i < splits.length; ++i) {
    str = str.replace(new RegExp(splits[i], "g"), split);
  }
  return str.split(split);
}

/**
 * 生成唯一的uuid
 * @return {str} uuid
 */
export function generateUUID() {
  let d = new Date().getTime();
  if (window.performance && typeof window.performance.now === "function") {
    d += performance.now();
  }
  const uuid = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (d + Math.random() * 16) % 16 | 0;
    d = Math.floor(d / 16);
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
  return uuid;
}

/**
 * 文件流处理
 */

function downLoadFile(ret, fileName) {
  const data = ret.data;
  if (data.type === "application/json") return;
  let contentDisposition = ret.headers["content-disposition"];
  contentDisposition = decodeURI(contentDisposition);
  const patt = new RegExp("fileName=([^;]+\\.[^\\.;]+);*");
  const result = patt.exec(contentDisposition);
  const url = window.URL.createObjectURL(
    new Blob([data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    })
  );
  const link = document.createElement("a");
  link.style.display = "none";
  link.href = url;
  link.setAttribute("download", result ? result[1] : fileName);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

function downLoadFileOrder(ret, fileName) {
  const blob = new Blob([ret.data]);
  const reader = new FileReader();
  reader.readAsText(blob, "utf-8");
  reader.onload = () => {
    try {
      const result = JSON.parse(reader.result);
      if (result?.msg) {
        message.error(result.msg);
        return;
      }
      downLoadFile(ret, fileName);
    } catch (err) {
      console.error(err);
      downLoadFile(ret, fileName);
    }
  };
}

/**
 * 用url的形式下载文件
 * @param {*} url 文件地址
 * @param {*} fileName 文件名
 */
export function downloadFileByUrl(url) {
  const regex = /^(https?)/i;
  const hasProtocol = regex.test(url);
  let targetUrl = url;

  if (hasProtocol) {
    targetUrl = window.location.protocol + url.split(":")[1];
  } else {
    targetUrl = `${window.location.protocol}//${url}`;
  }

  window.location.href = targetUrl;
}

// 参数类型是formData的请求
export function getExcelFile(url, formData) {
  axios
    .post(API_PREFIX + url, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
        token: getToken(),
        session_id: getSession(),
        source: platform,
      },
      timeout: 1 * 60 * 60 * 1000,
      responseType: "blob",
    })
    .then((res) => downLoadFile(res));
}

// 参数类型是json的请求
export function getExcelFileByparams(url, params, fileName) {
  return axios
    .post(API_PREFIX + url, params, {
      headers: {
        token: getToken(),
        session_id: getSession(),
        source: platform,
      },
      timeout: 1 * 60 * 60 * 1000,
      responseType: "blob",
    })
    .then((res) => downLoadFile(res, fileName));
}

// 参数方法是get的请求
export function getExcelFileByGet(url, fileName) {
  return axios
    .get(API_PREFIX + url, {
      headers: {
        token: getToken(),
        session_id: getSession(),
        source: platform,
      },
      timeout: 1 * 60 * 60 * 1000,
      responseType: "blob",
    })
    .then((res) => downLoadFile(res, fileName));
}

// 参数类型是json的请求订单列表
export function getExcelFileByparamsOrder(url, params, fileName) {
  return axios
    .post(API_PREFIX + url, params, {
      headers: {
        token: getToken(),
        session_id: getSession(),
        source: platform,
      },
      timeout: 1 * 60 * 60 * 1000,
      responseType: "blob",
    })
    .then((res) => {
      downLoadFileOrder(res, fileName);
    });
}

/**
 * 将字符串转换成模板字符串
 * @param {*} str 需要转换的字符串
 * @param {*} arguments 替换的变量
 */

// eslint-disable-next-line no-extend-native
String.prototype.interpolate = function (params) {
  const names = Object.keys(params);
  const vals = Object.values(params);
  // eslint-disable-next-line
  return new Function(...names, `return \`${this}\`;`)(...vals);
};

export function toTemplateStr(str, vars) {
  if (!str) return str;
  const result = str.interpolate(vars);
  return result;
}

/**
 * 解决Vue中不能在template中使用可选链的问题
 * @param {*} obj
 * @param  {...any} rest
 * @returns
 */
export const optionalChaining = (obj, ...rest) => {
  let tmp = obj;
  for (const key in rest) {
    const name = rest[key];
    tmp = tmp?.[name];
  }
  return tmp || "";
};

/**
 * 简单地去除对象中的空值，如''和null
 * @param {*} obj 目标对象
 * @returns
 */
export function clearEmptyPro(obj) {
  if (obj && Object.keys(obj).length > 0) {
    for (const key in obj) {
      if (obj[key] === "" || obj[key] === null) {
        delete obj[key];
      }
    }
  } else {
    return {};
  }
}

/**
 * 一维数组打包成二维数组
 */
export function chunkArray(array, chunkSize) {
  return array.reduce((result, item, index) => {
    const chunkIndex = Math.floor(index / chunkSize);
    if (!result[chunkIndex]) {
      result[chunkIndex] = []; // start a new chunk
    }
    result[chunkIndex].push(item);
    return result;
  }, []);
}

/**
 * 身份证校验
 */
export function idValidate(id) {
  if (!id) return false;
  for (const [key, regex] of Object.entries(idRegex)) {
    if (regex.test(id)) {
      return true;
    }
  }
  return false;
}

/**
 * @description 将两个对象的并集保留并返回(使用第二个对象的值)
 * @param {*} obj1
 * @param {*} obj2
 * @returns
 */
export function getCommonKeysUnion(obj1, obj2) {
  console.log("getCommonKeysUnion", obj1, obj2);

  const result = {};

  for (const key in obj1) {
    if (obj2.hasOwnProperty(key)) {
      result[key] = obj2[key];
    }
  }

  return result;
}

/**
 * @description 检查字符串中的链接前后是否有空格
 * @param {*} str 字符串
 * @returns Boolean
 */
export function checkLinksWithSpaces(str) {
  let matches = str.match(linkRegexp);

  if (!matches) {
    return true; // 或根据需求返回其他值，表示没有链接
  }

  for (let match of matches) {
    let index = str.indexOf(match);

    // 检查前面是否有空格或位于开头
    let hasSpaceBefore = index === 0 || str[index - 1] === " ";

    // 检查后面是否有空格或位于结尾
    let hasSpaceAfter =
      index + match.length === str.length || str[index + match.length] === " ";

    if (!hasSpaceBefore || !hasSpaceAfter) {
      return false;
    }
  }

  return true;
}

/**
 * @description 获取url的blob数据流
 */
export function getUrlBlob(url) {
  return new Promise(async (resolve) => {
    const httpsUrl = url.replace(/^http:/, 'https:'); // 替换http为https
    const response = await fetch(httpsUrl);
    const blob = await response.blob();

    resolve(blob);
  });
}

/**
 * @description 复制
 */
export async function copyImgageToClipboard(url) {
  if (!url) return;
  const blob = await getUrlBlob(url);
  const item = new ClipboardItem({ "image/png": blob });
  navigator.clipboard
    .write([item])
    .then(() => {
      message.success("复制成功");
    })
    .catch((err) => {
      message.error("复制失败");
    });
}

/**
 * @description 下载图片
 */
export async function downloadImage(url, filename) {
  if (!url) return;
  const blob = await getUrlBlob(url);
  const bUrl = window.URL.createObjectURL(blob);
  if (!bUrl) return;
  const link = document.createElement("a");
  link.href = bUrl; // 设置下载链接
  link.download = filename; // 设置下载文件名
  link.target = "_blank";
  document.body.appendChild(link);
  link.click(); // 触发下载
  document.body.removeChild(link); // 移除链接元素
}

/**
 * 树结构，根据一个属性找对应项
 */
export function findItemByAttr(arr, attr, value) {
  for (const item of arr) {
    if (item[attr] === value) return item;
    if (item?.children?.length) {
      const childItem = findItemByAttr(item.children, attr, value)
      if (childItem) return childItem
    }
  }

  return null
}

export default {
  toTemplateStr,
  optionalChaining,
  chunkArray,
  checkLinksWithSpaces,
  copyImgageToClipboard,
  downloadImage,
};
