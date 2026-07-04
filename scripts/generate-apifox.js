const fs = require('fs')
const path = require('path')

const server = process.env.SKY_SERVER_URL || 'http://127.0.0.1:8080'
const docsDir = path.resolve(__dirname, '..', 'docs')

const targets = {
  admin: {
    output: path.join(docsDir, 'ADMIN_API_APIFOX.json'),
    title: 'Takeout Guys 管理端 API',
    description: '管理后台接口，可直接导入 Apifox。请求头 token 用于管理员身份认证。',
    securityName: 'AdminToken',
    headerName: 'token'
  },
  user: {
    output: path.join(docsDir, 'USER_API_APIFOX.json'),
    title: 'Takeout Guys 用户端 API',
    description: '微信小程序用户接口，可直接导入 Apifox。请求头 authentication 用于用户身份认证；包含支付回调接口。',
    securityName: 'UserToken',
    headerName: 'authentication'
  }
}

async function getSwagger(group) {
  const response = await fetch(`${server}/v2/api-docs?group=${encodeURIComponent(group)}`)
  if (!response.ok) throw new Error(`${group}: HTTP ${response.status}`)
  return response.json()
}

function belongsToClient(apiPath, client) {
  if (client === 'admin') return apiPath.startsWith('/admin/')
  return apiPath.startsWith('/user/') || apiPath.startsWith('/notify/')
}

function referencedDefinitions(value) {
  const references = new Set()
  const visit = node => {
    if (!node || typeof node !== 'object') return
    if (typeof node.$ref === 'string' && node.$ref.startsWith('#/definitions/')) {
      references.add(decodeURIComponent(node.$ref.slice('#/definitions/'.length)))
    }
    Object.values(node).forEach(visit)
  }
  visit(value)
  return references
}

function selectDefinitions(paths, definitions = {}) {
  const required = referencedDefinitions(paths)
  const pending = [...required]
  while (pending.length) {
    const name = pending.pop()
    for (const dependency of referencedDefinitions(definitions[name])) {
      if (!required.has(dependency)) {
        required.add(dependency)
        pending.push(dependency)
      }
    }
  }
  return Object.fromEntries(Object.entries(definitions).filter(([name]) => required.has(name)))
}

function splitSnapshot(snapshot, client) {
  const paths = Object.fromEntries(
    Object.entries(snapshot.paths || {}).filter(([apiPath]) => belongsToClient(apiPath, client))
  )
  const usedTags = new Set(
    Object.values(paths).flatMap(pathItem =>
      Object.values(pathItem).flatMap(operation => operation && operation.tags ? operation.tags : [])
    )
  )
  return {
    ...snapshot,
    tags: (snapshot.tags || []).filter(tag => usedTags.has(tag.name)),
    paths,
    definitions: selectDefinitions(paths, snapshot.definitions)
  }
}

function normalizeDocument(document, client) {
  const target = targets[client]
  return {
    ...document,
    info: {
      ...(document.info || {}),
      title: target.title,
      description: target.description,
      version: '3.0'
    },
    host: 'localhost:8080',
    basePath: '/',
    schemes: ['http'],
    securityDefinitions: {
      [target.securityName]: { type: 'apiKey', name: target.headerName, in: 'header' }
    }
  }
}

async function loadDocuments() {
  try {
    const snapshot = await getSwagger('全部后端接口')
    return {
      admin: splitSnapshot(snapshot, 'admin'),
      user: splitSnapshot(snapshot, 'user')
    }
  } catch (error) {
    const hasExistingDocuments = Object.values(targets).every(target => fs.existsSync(target.output))
    if (!hasExistingDocuments) throw error
    console.warn(`Server unavailable; keeping existing client documents: ${error.message}`)
    return {
      admin: splitSnapshot(JSON.parse(fs.readFileSync(targets.admin.output, 'utf8')), 'admin'),
      user: splitSnapshot(JSON.parse(fs.readFileSync(targets.user.output, 'utf8')), 'user')
    }
  }
}

async function main() {
  const documents = await loadDocuments()
  for (const client of Object.keys(targets)) {
    const document = normalizeDocument(documents[client], client)
    fs.writeFileSync(targets[client].output, `${JSON.stringify(document, null, 2)}\n`, 'utf8')
    console.log(`Generated ${path.relative(process.cwd(), targets[client].output)}: ${Object.keys(document.paths || {}).length} paths`)
  }
}

main().catch(error => {
  console.error(error.message)
  process.exit(1)
})
