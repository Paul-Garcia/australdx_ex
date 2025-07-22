const io = require('socket.io')(8080, {
  cors: { origin: "*" }
});

io.on('connection', (socket) => {
  console.log('Client connecté');

  const intervalId = setInterval(() => {
    const buffer = generatePointCloudBuffer();
    socket.emit('pointCloud', buffer);
  }, 100); // toutes les 100ms

  socket.on('disconnect', () => {
    clearInterval(intervalId);
    console.log('Client déconnecté');
  });
});

function generatePointCloudBuffer() {
  const numPoints = 50000;
  const buffer = Buffer.alloc(numPoints * 3 * 4); // 3 floats (x, y, z) * 4 bytes chacun

  for (let i = 0; i < numPoints; i++) {
    const x = (Math.random() * 4 - 2); // de -2 à +2
    const y = (Math.random() * 4 - 2);
    const z = (Math.random() * 6 - 3); // de -3 à +3

    buffer.writeFloatLE(x, i * 12 + 0);
    buffer.writeFloatLE(y, i * 12 + 4);
    buffer.writeFloatLE(z, i * 12 + 8);
  }

  return buffer;
}
  
console.log('Serveur Socket.IO prêt sur le port 8080');