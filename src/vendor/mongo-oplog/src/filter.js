'use strict'

libs = require('./libs')
const events = {
  i: 'insert',
  u: 'update',
  d: 'delete'
}

function regex(pattern) {
  pattern = pattern || '*'
  pattern = pattern.replace(/[*]/g, '(.*?)')
  return new RegExp(`^${pattern}$`, 'i')
}

module.exports = (ns, oplog) => {
  const Emitter = new libs.eventemitter3()
  const debug = libs.debug('mongo-oplog:filter')
  const filter = new Emitter()
  const re = regex(ns)

  debug('initializing filter with re %s', ns)

  function onop(doc) {
    if (!re.test(doc.ns) || filter.ignore) return
    debug('incoming data %j', doc)
    filter.emit('op', doc)
    if (events[doc.op]) {
      filter.emit(events[doc.op], doc)
    }
  }

  function destroy() {
    debug('removing filter bindings')
    oplog.removeListener('op', onop)
    filter.removeAllListeners()
  }

  oplog.on('op', onop)

  return Object.assign(filter, { destroy })
}

module.exports.regex = regex
