const frame = document.getElementById('frame') as HTMLImageElement;
const stats = document.getElementById('stats') as HTMLDivElement;

// Sample base64 encoded processed frame (dummy edge-detected image)
const sampleBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
frame.src = sampleBase64;
stats.innerText = `FPS: 15\nResolution: 640x480`;

