
const {app, BrowserWindow} = require("electron");


function createWindow() {
    const window = new BrowserWindow({
        width: 800,
        height: 600
    });

    window.loadURL("http://localhost:5500")
    console.log("Electron loading frontend from http://localhost:5500");
}


// Close app if all windows are closed
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})

// Create new window
app.whenReady().then(() => {
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
});
