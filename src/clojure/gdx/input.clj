(ns clojure.gdx.input)

(defprotocol Input
  (x [_] "The x coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner.")

  (y [_] "The y coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner.")

  (set-processor! [_ input-processor] "Sets the `InputProcessor` that will receive all touch and key input events. It will be called before the `render!` function each frame.")

  (button-just-pressed? [_ button] "Returns whether a given button has just been pressed. On Android only the `:left` button is meaningful before version 4.0.")

  (key-just-pressed? [_ key] "Returns whether the key has just been pressed.")

  (key-pressed? [_ key] "Returns whether the key is pressed."))
