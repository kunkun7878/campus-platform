const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8124;

const mime = {
  '.html': 'text/html',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
};

http.createServer((req, res) => {
  let filePath = '.' + req.url.split('?')[0];
  if (filePath === './') filePath = './campus-miniapp-prototype.html';
  const ext = path.extname(filePath);
  const contentType = mime[ext] || 'text/html';
  fs.readFile(filePath, (err, data) => {
    if (err) { res.writeHead(404); res.end('Not found'); return; }
    res.writeHead(200, { 'Content-Type': contentType });
    res.end(data);
  });
}).listen(PORT, () => console.log(`http://127.0.0.1:${PORT}`));
