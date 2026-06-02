#!/usr/bin/env node
import minecraftProtocol from 'minecraft-protocol'
import { adapterReport, resolveMineflayerVersion } from './protocol_adapter.js'

function parseArgs (argv) {
  const out = { _: [] }
  for (let i = 2; i < argv.length; i++) {
    const arg = argv[i]
    if (arg.startsWith('--')) {
      const key = arg.slice(2)
      const next = argv[i + 1]
      if (next == null || next.startsWith('--')) out[key] = true
      else { out[key] = next; i++ }
    } else out._.push(arg)
  }
  return out
}

function emit (event, payload = {}) {
  process.stdout.write(JSON.stringify({ ts: new Date().toISOString(), event, ...payload }) + '\n')
}

async function pingServer ({ host, port, timeoutMs }) {
  const ms = Number(timeoutMs ?? 5000)
  return await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`ping timeout after ${ms}ms`)), ms)
    try {
      minecraftProtocol.ping({ host, port: Number(port), closeTimeout: ms }, (err, response) => {
        clearTimeout(timer)
        if (err) reject(err)
        else resolve(response)
      })
    } catch (err) {
      clearTimeout(timer)
      reject(err)
    }
  })
}

async function probe (args) {
  const host = args.host ?? '127.0.0.1'
  const port = Number(args.port ?? 25565)
  const requestedVersion = args.version ?? '26.1.2'
  try {
    const ping = await pingServer({ host, port, timeoutMs: args['timeout-ms'] ?? 5000 })
    const report = adapterReport(requestedVersion, ping?.version?.name)
    emit('probe_ok', { host, port, ping, adapter: report })
    return 0
  } catch (err) {
    emit('probe_failed', { host, port, adapter: adapterReport(requestedVersion), error: String(err?.stack ?? err) })
    return 2
  }
}

function parseCommands (args) {
  if (args['commands-json']) return JSON.parse(args['commands-json'])
  if (args.command) return [args.command]
  return []
}

async function runBot (args) {
  const host = args.host ?? '127.0.0.1'
  const port = Number(args.port ?? 25565)
  const username = args.username ?? 'EbbBot'
  const requestedVersion = args.version ?? '26.1.2'
  const timeoutMs = Number(args['timeout-ms'] ?? 30000)
  const commands = parseCommands(args)
  let pingVersionName = null
  try {
    const ping = await pingServer({ host, port, timeoutMs: Math.min(timeoutMs, 5000) })
    pingVersionName = ping?.version?.name ?? null
    emit('ping_ok', { ping })
  } catch (err) {
    emit('ping_warning', { error: String(err?.message ?? err) })
  }
  const resolution = resolveMineflayerVersion(requestedVersion, pingVersionName)
  emit('adapter_resolution', resolution)

  const mineflayer = await import('mineflayer')
  return await new Promise(resolve => {
    const bot = mineflayer.default.createBot({
      host,
      port,
      username,
      auth: 'offline',
      version: resolution.selectedVersion
    })
    let done = false
    const finish = (code, detail = {}) => {
      if (done) return
      done = true
      emit('finish', { code, ...detail })
      try { bot.quit('ebb gui automation finished') } catch (_) {}
      resolve(code)
    }
    const timer = setTimeout(() => finish(3, { reason: 'timeout' }), timeoutMs)
    bot.once('spawn', async () => {
      emit('spawn', { entityId: bot.entity?.id, position: bot.entity?.position })
      try {
        for (const command of commands) {
          emit('chat_send', { command })
          bot.chat(command)
          await new Promise(r => setTimeout(r, Number(args['command-delay-ms'] ?? 750)))
        }
        clearTimeout(timer)
        finish(0, { reason: 'commands_sent', commands: commands.length })
      } catch (err) {
        clearTimeout(timer)
        finish(4, { reason: 'command_error', error: String(err?.stack ?? err) })
      }
    })
    bot.on('message', message => emit('message', { text: message.toString() }))
    bot.on('kicked', reason => { clearTimeout(timer); finish(5, { reason: 'kicked', detail: String(reason) }) })
    bot.on('error', err => emit('bot_error', { error: String(err?.stack ?? err) }))
    bot.on('end', reason => { if (!done) { clearTimeout(timer); finish(6, { reason: 'ended', detail: String(reason) }) } })
  })
}

async function selfTest () {
  emit('self_test', { adapter: adapterReport('26.1.2') })
  return 0
}

async function main () {
  const args = parseArgs(process.argv)
  if (args['self-test']) return await selfTest()
  const command = args._[0] ?? 'help'
  if (command === 'probe') return await probe(args)
  if (command === 'run') return await runBot(args)
  emit('usage', { commands: ['probe', 'run', '--self-test'] })
  return command === 'help' ? 0 : 1
}

main().then(code => process.exit(code)).catch(err => {
  emit('fatal', { error: String(err?.stack ?? err) })
  process.exit(99)
})
