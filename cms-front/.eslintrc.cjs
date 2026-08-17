module.exports = {
  'env': {
    'browser': true,
    'es2021': true
  },
  'extends': [
    './.eslintrc-auto-import.json',
    'eslint:recommended',
    'plugin:vue/vue3-essential'
  ],
  'overrides': [
    {
      'env': {
        'node': true
      },
      'files': [
        '.eslintrc.{js,cjs}'
      ],
      'parserOptions': {
        'sourceType': 'script'
      }
    }
  ],
  'parserOptions': {
    'ecmaVersion': 'latest',
    'sourceType': 'module'
  },
  'plugins': [
    'vue'
  ],
  'ignorePatterns': ['vite.config.js', '.eslintrc.cjs', 'auto-imports.d.ts'],
  'rules': {
    'no-debugger': process.env.ENV === 'production' ? 'error' : 'off',
    'vue/multi-word-component-names': 'off', // 禁止使用连字符命名
    'indent': [
      'warn',
      2
    ],
    'linebreak-style': [
      'error',
      'windows'
    ],
    'quotes': [ // 字符串使用单引号
      'warn',
      'single'
    ],
    'semi': [ // 语句强制不使用分号结尾
      'warn',
      'never'
    ],
    'vue/no-unused-vars': [ // 禁止未使用变量
      'error',
      {
        'ignorePattern': '^_'
      }
    ],
    'vue/no-use-v-if-with-v-for': ['error', { // 禁止使用v-if和v-for同时使用
      'allowUsingIterationVar': false
    }],
    'vue/max-attributes-per-line': ['error', { // html属性换行 单行最大四个 多行每行最多一个
      'singleline': {
        'max': 4
      },      
      'multiline': {
        'max': 1
      }
    }],
    'vue/first-attribute-linebreak': ['error', { // 第一个属性换行
      'singleline': 'ignore',
      'multiline': 'below'
    }],
    'vue/html-closing-bracket-newline': ['error', { // html标签闭合符换行
      'singleline': 'never',
      'multiline': 'always'
    }],
    'vue/html-indent': ['warn', 2], // html缩进 2个空格
    'vue/mustache-interpolation-spacing': ['warn', 'always'], // 插值前后空格
    'vue/no-multi-spaces': ['error', { // 禁止多个空格
      'ignoreProperties': false
    }],
    'vue/prop-name-casing': ['warn', 'camelCase'], // 属性名驼峰命名
    'vue/v-on-event-hyphenation': ['error', 'never', {
      'autofix': false,
      'ignore': []
    }]
  }
}
