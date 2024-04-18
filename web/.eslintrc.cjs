/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution')

module.exports = {
  root: true,
  'extends': [
    'plugin:vue/vue3-essential',
    'eslint:recommended',
    '@vue/eslint-config-typescript',
    '@vue/eslint-config-prettier/skip-formatting',
    '@vue/typescript/recommended',
    'plugin:import/recommended',
    'plugin:import/typescript'
  ],
  rules: {
    'vue/no-unsupported-features': ['error', {
      'version': '3.3.11',
      'ignores': []
    }],
    'import/order': [
      'warn',
      {
        'newlines-between': 'always',
        'alphabetize': {
          'order': 'asc',
          'caseInsensitive': true
        },
      }
    ],
    'import/no-unresolved': 'off',
    '@typescript-eslint/ban-ts-comment': 'warn',
    '@typescript-eslint/ban-types': 'warn',
    '@typescript-eslint/no-empty-interface': 'warn',
    '@typescript-eslint/no-inferrable-types': 'off',
    '@typescript-eslint/no-this-alias': 'warn',
    '@typescript-eslint/no-var-requires': 'warn',
    '@typescript-eslint/explicit-module-boundary-types': 'warn',
    '@typescript-eslint/explicit-function-return-type': ['warn',
      {
        allowTypedFunctionExpressions: false,
      }],
    '@typescript-eslint/no-shadow': ['error'],
    'space-before-function-paren': ['error', {
      'anonymous': 'never',
      'named': 'never',
      'asyncArrow': 'always'
    }],
    'no-shadow': 'off',
    'getter-return': 'error',
    'max-len': ['error', 200, { 'ignorePattern': 'd="([\\s\\S]*?)"', 'ignoreTrailingComments': true, 'ignoreComments': true }],
    'no-console': 'error',
    'no-debugger': 'warn',
    'no-empty': 'error',
    'no-extra-boolean-cast': 'warn',
    'no-inferrable-types': 'off',
    'no-irregular-whitespace': 2,
    'no-prototype-builtins': 'error',
    'no-trailing-spaces': 'error',
    'no-undef': 'warn',
    'prefer-const': 'error',
    'vue/no-dupe-keys': 'warn',
    'prefer-spread': 'warn',
    'no-unreachable': 'warn',
    '@typescript-eslint/no-unused-vars': ['warn', { 'argsIgnorePattern': '^_', 'varsIgnorePattern': '^_' }],
    'no-unused-vars': 'off', // It's turned off because it conflicts with the '@typescript-eslint/no-unused-vars' rule
    'no-useless-escape': 'error',
    'no-var': 'error',
    'semi': 'error',
    'padding-line-between-statements': [
      'error',
      { 'blankLine': 'always', 'prev': '*', 'next': 'return' },
      { 'blankLine': 'always', 'prev': '*', 'next': 'switch' },
      { 'blankLine': 'always', 'prev': '*', 'next': 'if' },
      { 'blankLine': 'always', 'prev': 'block-like', 'next': '*' },
      { 'blankLine': 'always', 'prev': ['const', 'let', 'var'], 'next': '*' },
      { 'blankLine': 'any',    'prev': ['const', 'let', 'var'], 'next': ['const', 'let', 'var'] }
    ],
    'vue/script-setup-uses-vars': 'error',
    'vue/no-parsing-error': 'error',
    'vue/no-use-v-if-with-v-for': 'error',
    'object-curly-spacing': [2, 'always'],
    'vue/object-curly-spacing': ['error', 'always'],
    'vue/require-v-for-key': 'error',
    'vue/no-multi-spaces': 'error',
    'vue/component-tags-order': ['warn', {
      'order': [ [ 'script', 'template' ], 'style' ]
    }],
    'vue/padding-line-between-blocks': 'warn',
    'vue/valid-template-root': 'error',
    'vue/valid-v-for': 'warn',
    'vue/valid-v-text': 'error',
    quotes: ['error', 'single'],
    'vue/max-attributes-per-line': [
      'error',
      {
        singleline: 1,
        multiline: {
          max: 1,
        }
      }
    ],
    'vue/first-attribute-linebreak': ['warn', {
      singleline: 'ignore',
      multiline: 'below'
    }],
    'vue/script-indent': [
      'error',
      2,
      {
        baseIndent: 1,
        switchCase: 1
      }
    ],
    'vue/component-name-in-template-casing': 'warn',
    'vue/multi-word-component-names': 'warn',
    'vue/no-useless-template-attributes': 'warn',
    'vue/html-closing-bracket-newline': ['warn', {
      'singleline': 'never',
      'multiline': 'never'
    }],
    'vue/html-indent': [
      2,
      2,
      {
        attribute: 1,
        baseIndent: 1,
        closeBracket: 0,
        alignAttributesVertically: true,
        ignores: []
      }
    ],
    'vue/no-required-prop-with-default': ['error', {
      'autofix': false,
    }]
  },
  overrides: [],
  parserOptions: {
    ecmaVersion: 'latest'
  },
  ignorePatterns: [
    '**/.vscode/*',
    '**/*.css',
    '**/*.min.js',
    '**/dist/*',
    '**/licenses/*',
    '**/locale/*',
    '**/misc/*',
    '**/node_modules/*',
    '**/cypress/*',
    '**/patches/*',
    '**/public/*',
    '**/src/assets/*',
    '**/src/vendor/*',
    '**/tests/*',
    'babel.config.js',
    'jest.config.js',
    'postcss.config.js',
    'tailwind.js',
    'vc-trade.ts',
    'vue.config.js'
  ],
}