const frame = document.getElementById('frame');
const stats = document.getElementById('stats');
// Replace this with actual base64 image from Android native output
const sampleBase64 = "data:image/png;base64,REPLACE_WITH_BASE64";
frame.src = sampleBase64;
stats.innerText = `FPS: 12\nResolution: 640x480`;
