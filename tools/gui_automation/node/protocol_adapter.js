import { createRequire } from 'module'
import mcData from 'minecraft-data'

const require = createRequire(import.meta.url)

const DATA_ALIAS_BY_MAJOR = new Map([
  ['26.1', '1.21.11'],
  ['26.2-snapshot-7', '1.21.11'],
  ['26.2-snapshot-6', '1.21.11']
])

let aliasesInstalled = false

export function installHighVersionDataAliases () {
  if (aliasesInstalled) return { installed: false, aliases: Object.fromEntries(DATA_ALIAS_BY_MAJOR) }
  const data = require('minecraft-data/data.js')
  const protocolVersions = require('minecraft-data/minecraft-data/data/pc/common/protocolVersions.json')
  for (const [targetMajor, sourceMajor] of DATA_ALIAS_BY_MAJOR.entries()) {
    if (!data.pc[targetMajor] && data.pc[sourceMajor]) {
      const source = JSON.parse(JSON.stringify(data.pc[sourceMajor]))
      const exact = protocolVersions.find(v => v.minecraftVersion === targetMajor)
      const byMajor = protocolVersions.find(v => v.majorVersion === targetMajor && v.releaseType === 'release')
        ?? protocolVersions.find(v => v.majorVersion === targetMajor)
      const meta = exact ?? byMajor
      source.version = {
        ...source.version,
        version: meta?.version ?? source.version.version,
        minecraftVersion: meta?.minecraftVersion ?? targetMajor,
        majorVersion: meta?.majorVersion ?? targetMajor,
        releaseType: meta?.releaseType ?? 'release'
      }
      data.pc[targetMajor] = source
    }
  }
  aliasesInstalled = true
  return { installed: true, aliases: Object.fromEntries(DATA_ALIAS_BY_MAJOR) }
}

export function supportedVersions () {
  try {
    const versions = mcData.versions?.pc ?? []
    return versions.map(v => v.minecraftVersion).filter(Boolean)
  } catch (err) {
    return []
  }
}

export function hasDataForVersion (version) {
  if (!version) return false
  installHighVersionDataAliases()
  try {
    return Boolean(mcData(version))
  } catch (err) {
    return false
  }
}

export function resolveMineflayerVersion (requestedVersion, pingVersionName = null) {
  installHighVersionDataAliases()
  const candidates = []
  if (requestedVersion) candidates.push(requestedVersion)
  if (pingVersionName) candidates.push(String(pingVersionName).replace(/^Minecraft\s+/i, ''))
  for (const candidate of candidates) {
    if (hasDataForVersion(candidate)) {
      const data = mcData(candidate)
      return {
        requestedVersion,
        pingVersionName,
        selectedVersion: candidate,
        exact: candidate === requestedVersion || candidate === pingVersionName,
        protocolVersion: data?.version?.version ?? null,
        dataVersion: data?.version?.dataVersion ?? null,
        reason: 'exact protocol metadata exists; data layer supplied by exact data or installed high-version alias'
      }
    }
  }
  return {
    requestedVersion,
    pingVersionName,
    selectedVersion: false,
    exact: false,
    protocolVersion: null,
    dataVersion: null,
    reason: 'no exact or aliased data entry; let mineflayer/minecraft-protocol auto-negotiate and report failure if unsupported'
  }
}

export function adapterReport (requestedVersion = '26.1.2', pingVersionName = null) {
  const aliasReport = installHighVersionDataAliases()
  const versions = supportedVersions()
  const resolution = resolveMineflayerVersion(requestedVersion, pingVersionName)
  return {
    requestedVersion,
    pingVersionName,
    newestKnownProtocolVersion: versions[0] ?? null,
    oldestKnownProtocolVersion: versions.at(-1) ?? null,
    supportedVersionCount: versions.length,
    selectedVersion: resolution.selectedVersion,
    exact: resolution.exact,
    protocolVersion: resolution.protocolVersion,
    dataVersion: resolution.dataVersion,
    reason: resolution.reason,
    aliases: aliasReport.aliases,
    notes: [
      'minecraft-data includes 26.x protocol metadata but not always full per-version data tables.',
      'The adapter aliases 26.1 data tables to the newest compatible 1.21.x tables while preserving the 26.1.2 protocolVersion/dataVersion metadata.',
      'For Ebb GUI E2E, mineflayer is used for server probing/chat/state while screenshot automation remains visual authority.'
    ]
  }
}
