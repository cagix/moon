(ns clojure.input
  "
   screen and the accelerometer. On some backends (desktop, gwt, etc) the touch screen is replaced by mouse input. The accelerometer is of course not available on all backends.

Instead of polling for events, one can process all input events with an InputProcessor. You can set the InputProcessor via the setInputProcessor(InputProcessor) method. It will be called before the ApplicationListener.render() method in each frame.

Keyboard keys are translated to the constants in Input.Keys transparently on all systems. Do not use system specific key constants.

The class also offers methods to use (and test for the presence of) other input systems like vibration, compass, on-screen keyboards, and cursor capture. Support for simple input dialogs is also provided.
  "
  )

(defprotocol Input
  (button-just-pressed? [_ button]
                        "


Returns whether a given button has just been pressed. Button constants can be found in Input.Buttons. On Android only the Buttons#LEFT constant is meaningful before version 4.0. On WebGL (GWT), only LEFT, RIGHT and MIDDLE buttons are supported.

Parameters:
    button - the button to check.
Returns:
    true or false.
                        "
                        )
  (key-pressed? [_ key]
                "Returns whether the key is pressed.

Parameters:
    key - The key code as found in Input.Keys.
Returns:
    true or false.
                "
                )
  (key-just-pressed? [_ key]
                     "Returns whether the key has just been pressed.

Parameters:
    key - The key code as found in Input.Keys.
Returns:
    true or false.
                     "
                     )
  (set-processor! [_ input-processor]
                  "
Sets the InputProcessor that will receive all touch and key input events. It will be called before the ApplicationListener.render() method each frame.

Parameters:
    processor - the InputProcessor "

                  )
  (mouse-position [_]
                  "
    Returne sThe `[x y]` coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner."
                  ))
