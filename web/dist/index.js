"use strict";
// TypeScript web viewer for edge-detected frames
Object.defineProperty(exports, "__esModule", { value: true });
exports.initViewer = initViewer;
exports.updateStats = updateStats;
exports.bytesToBase64 = bytesToBase64;
const frame = document.getElementById('frame');
const stats = document.getElementById('stats');
// Sample base64 encoded processed frame (dummy edge-detected image)
// This is a 1x1 pixel placeholder - replace with actual processed frame
const sampleBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFUlEQVR42mNk+M9Qz0AEYBxVSF+FAP0RBgFP2BvLAAAAAElFTkSuQmCC";
// Initialize the viewer
function initViewer() {
    frame.src = sampleBase64;
    updateStats(15, 640, 480, 33.5);
}
// Update stats display
function updateStats(fps, width, height, processingTime) {
    stats.innerText = `FPS: ${fps}\nResolution: ${width}x${height}\nProcessing Time: ${processingTime.toFixed(1)} ms`;
}
// Function to convert ByteArray to Base64 (for future integration)
// This would be called when receiving frames from Android app
function bytesToBase64(bytes) {
    let binary = '';
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
}
// Simulated frame update (for demonstration)
function simulateFrameUpdate() {
    // In production, this would receive data from WebSocket or HTTP endpoint
    const mockFps = 15 + Math.random() * 10;
    const mockProcessingTime = 30 + Math.random() * 20;
    updateStats(Math.floor(mockFps), 640, 480, mockProcessingTime);
}
// Initialize on page load
initViewer();
// Simulate updates every 2 seconds (optional, for demo purposes)
setInterval(simulateFrameUpdate, 2000);
//# sourceMappingURL=index.js.map