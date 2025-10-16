const sqlite3 = require('sqlite3').verbose();
const db = new sqlite3.Database('./cache/messages.db');

db.serialize(() => {
  db.run(`
    CREATE TABLE IF NOT EXISTS messages (
      id TEXT PRIMARY KEY,
      jid TEXT,
      pushName TEXT,
      timestamp INTEGER,
      type TEXT,
      text TEXT,
      isGroup BOOLEAN,
      chatName TEXT,
      direction TEXT
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS queue (
      id TEXT PRIMARY KEY,
      to_jid TEXT,
      text TEXT,
      created_at INTEGER,
      status TEXT,
      retry_count INTEGER DEFAULT 0,
      error_message TEXT
    )
  `);
});

function insertMessage(summary) {
  const stmt = db.prepare('INSERT OR REPLACE INTO messages VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)');
  stmt.run(
    summary.id,
    summary.jid,
    summary.pushName,
    summary.timestamp,
    summary.type,
    summary.text,
    summary.isGroup,
    summary.chatName,
    summary.direction
  );
  stmt.finalize();
}

function addToQueue({ to, text }) {
  const stmt = db.prepare('INSERT INTO queue (id, to_jid, text, created_at, status) VALUES (?, ?, ?, ?, ?)');
  const id = require('crypto').randomUUID();
  stmt.run(id, to, text, Date.now(), 'pending');
  stmt.finalize();
}

function getPendingQueue() {
  return new Promise((resolve, reject) => {
    db.all("SELECT * FROM queue WHERE status = 'pending' ORDER BY created_at ASC", (err, rows) => {
      if (err) {
        reject(err);
      } else {
        resolve(rows.map(row => ({
          id: row.id,
          to: row.to_jid,
          text: row.text,
          created_at: row.created_at,
          status: row.status
        })));
      }
    });
  });
}

function markSent(id) {
  db.run("UPDATE queue SET status = 'sent' WHERE id = ?", id);
}

function markFailed(id, errorMessage) {
  db.run("UPDATE queue SET status = 'failed', error_message = ? WHERE id = ?", errorMessage, id);
}

module.exports = {
  insertMessage,
  addToQueue,
  getPendingQueue,
  markSent,
  markFailed,
};
