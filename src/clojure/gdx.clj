(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defmacro post-runnable
  "Posts a Runnable on the main loop thread. In a multi-window application, the Gdx.graphics and Gdx.input values may be unpredictable at the time the Runnable is executed. If graphics or input are needed, they can be copied to a variable to be used in the Runnable. For example:

  final Graphics graphics = Gdx.graphics;"
  [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn exit-app
  "Schedule an exit from the application. On android, this will cause a call to pause() and dispose() some time in the future, it will not immediately finish your application. On iOS this should be avoided in production as it breaks Apples guidelines"
  []
  (.exit Gdx/app))

(defn internal-file
  "Convenience method that returns a Files.FileType.Internal file handle."
  [path]
  (.internal Gdx/files path))

(defn frames-per-second
  "the average number of frames per second"
  []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time
  "the time span between the current frame and the last frame in seconds."
  []
  (.getDeltaTime Gdx/graphics))

(defn new-cursor
  "Create a new cursor represented by the Pixmap. The Pixmap must be in RGBA8888 format, width & height must be powers-of-two greater than zero (not necessarily equal) and of a certain minimum size (32x32 is a safe bet), and alpha transparency must be single-bit (i.e., 0x00 or 0xFF only). This function returns a Cursor object that can be set as the system cursor by calling setCursor(Cursor) .

  Parameters:

  pixmap - the mouse cursor image as a Pixmap

  xHotspot - the x location of the hotspot pixel within the cursor image (origin top-left corner)

  yHotspot - the y location of the hotspot pixel within the cursor image (origin top-left corner)

  Returns:
  a cursor object that can be used by calling setCursor(Cursor) or null if not supported"
  [pixmap [hotspot-x hotspot-y]]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor
  "Only viable on the lwjgl-backend and on the gwt-backend. Browsers that support cursor:url() and support the png format (the pixmap is converted to a data-url of type image/png) should also support custom cursors. Will set the mouse cursor image to the image represented by the Cursor. It is recommended to call this function in the main render thread, and maximum one time per frame.

  Parameters:
  cursor - the mouse cursor as a Cursor "
  [cursor]
  (.setCursor Gdx/graphics cursor))

(defn mouse-x
  "The x coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner.

  Returns int"
  []
  (.getX Gdx/input))

(defn mouse-y
  "The y coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner.

  Returns int"
  []
  (.getY Gdx/input))

(defn set-input-processor
  "Sets the InputProcessor that will receive all touch and key input events. It will be called before the ApplicationListener.render() method each frame."
  [processor]
  (.setInputProcessor Gdx/input processor))

(defn button-just-pressed?
  "Returns whether a given button has just been pressed. Button constants can be found in Input.Buttons. On Android only the Buttons#LEFT constant is meaningful before version 4.0.

  Parameters:
  button - the button to check.

  Returns:
  true or false."
  [b]
  (.isButtonJustPressed Gdx/input b))

(defn key-just-pressed?
  "Returns whether the key has just been pressed.

  Parameters:
  key - The key code as found in Input.Keys.

  Returns:
  true or false."
  [k]
  (.isKeyJustPressed Gdx/input k))

(defn key-pressed?
  "Returns whether the key is pressed.

  Parameters:
  key - The key code as found in Input.Keys.

  Returns:
  true or false. "
  [k]
  (.isKeyPressed Gdx/input k))
