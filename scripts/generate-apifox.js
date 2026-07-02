const fs = require('fs')

const server = process.env.SKY_SERVER_URL || 'http://127.0.0.1:8080'
const output = 'docs/BACKEND_API_APIFOX.json'

async function getSwagger(group) {
  const response = await fetch(`${server}/v2/api-docs?group=${encodeURIComponent(group)}`)
  if (!response.ok) throw new Error(`${group}: HTTP ${response.status}`)
  return response.json()
}

function mergeSwagger(admin, user) {
  const result = {
    swagger: '2.0',
    info: {
      title: 'Takeout Guys 全部后端接口',
      description: 'Apifox 可直接导入。由运行中的 Springfox 文档生成；AI 规划接口另见 AI_AGENT_API_APIFOX.json。',
      version: '2.0'
    },
    host: 'localhost:8080',
    basePath: '/',
    schemes: ['http'],
    consumes: ['application/json'],
    produces: ['application/json'],
    tags: [...(admin.tags || []), ...(user.tags || []), { name: '支付回调', description: '微信支付通知' }],
    paths: { ...(admin.paths || {}), ...(user.paths || {}) },
    definitions: { ...(admin.definitions || {}), ...(user.definitions || {}) },
    securityDefinitions: {
      AdminToken: { type: 'apiKey', name: 'token', in: 'header' },
      UserToken: { type: 'apiKey', name: 'authentication', in: 'header' }
    }
  }
  result.paths['/notify/paySuccess'] = {
    post: {
      tags: ['支付回调'],
      summary: '微信支付成功回调',
      consumes: ['application/json'],
      parameters: [{ name: 'body', in: 'body', required: true, schema: { type: 'object', additionalProperties: true } }],
      responses: { 200: { description: '回调处理结果' } }
    }
  }
  return result
}

async function main() {
  let document
  try {
    document = await getSwagger('全部后端接口')
  } catch (_) {
    const [admin, user] = await Promise.all([getSwagger('管理端接口'), getSwagger('用户端接口')])
    document = mergeSwagger(admin, user)
  }
  fs.writeFileSync(output, JSON.stringify(document, null, 2) + '\n', 'utf8')
  console.log(`Generated ${output}: ${Object.keys(document.paths || {}).length} paths`)
}

main().catch(error => {
  console.error(error.message)
  process.exit(1)
})
