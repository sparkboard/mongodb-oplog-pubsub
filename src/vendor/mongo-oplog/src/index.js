'use strict'

const createFilter = require('./filter')
const createStream = require('./stream')
const libs = require('./libs')
let oplogDebug = () => {}

const MONGO_URI = 'mongodb://127.0.0.1:27017/local'

const events = {
  i: 'insert',
  u: 'update',
  d: 'delete'
}

// Add callback support to promise
const toCb = fn => cb => {
  try {
    const val = fn(cb)
    if (!cb) return val
    else if (val && typeof val.then === 'function') {
      return val.then(val => cb(null, val)).catch(cb)
    }
    cb(null, val)
  } catch (err) {
    cb(err)
  }
}

module.exports = (uri, options = {}) => {
  let db
  let stream
  let connected = false
  const { ns, since, coll, ...opts } = options
  Object.assign(libs, options.libs)
  delete opts.libs
  oplogDebug = libs.debug('mongo-oplog')
  const Emitter = libs.eventemitter3
  const oplog = new Emitter()

  let ts = since || 0
  uri = uri || MONGO_URI

  if (typeof uri !== 'string') {
    if (uri && uri.collection) {
      db = uri
      connected = true
    } else {
      throw new Error('Invalid mongo db.')
    }
  }

  async function connect() {
    if (connected) return db
    db = await libs.mongodb.MongoClient.connect(uri, opts)
    connected = true
  }

  async function tail() {
    try {
      oplogDebug('Connected to oplog database')
      await connect()
      stream = await createStream({ ns, coll, ts, db })
      stream.on('end', onend)
      stream.on('data', ondata)
      stream.on('error', onerror)
      return stream
    } catch (err) {
      onerror(err)
    }
  }

  function filter(ns) {
    return createFilter(ns, oplog)
  }

  async function stop() {
    if (stream) stream.destroy()
    oplogDebug('streaming stopped')
    return oplog
  }

  async function destroy() {
    await stop()
    if (!connected) return oplog
    await db.close(true)
    connected = false
    return oplog
  }

  function ondata(doc) {
    if (oplog.ignore) return oplog
    oplogDebug('incoming data %j', doc)
    ts = doc.ts
    oplog.emit('op', doc)
    oplog.emit(events[doc.op], doc)
    return oplog
  }

  function onend() {
    oplogDebug('stream ended')
    oplog.emit('end')
    return oplog
  }

  function onerror(err) {
    if (/cursor (killed or )?timed out/.test(err.message)) {
      oplogDebug('cursor timeout - re-tailing %j', err)
      tail()
    } else {
      oplogDebug('oplog error %j', err)
      oplog.emit('error', err)
    }
  }

  return Object.assign(oplog, {
    db,
    filter,
    tail: toCb(tail),
    stop: toCb(stop),
    destroy: toCb(destroy)
  })
}

module.exports.events = events
module.exports.default = module.exports
