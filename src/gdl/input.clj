(ns gdl.input
  "
 * <p>
 * Interface to the input facilities. This allows polling the state of the keyboard, the touch screen and the accelerometer. On
 * some backends (desktop, gwt, etc) the touch screen is replaced by mouse input. The accelerometer is of course not available on
 * all backends.
 * </p>
 *
 * <p>
 * Instead of polling for events, one can process all input events with an {@link InputProcessor}. You can set the InputProcessor
 * via the {@link #setInputProcessor(InputProcessor)} method. It will be called before the {@link ApplicationListener#render()}
 * method in each frame.
 * </p>
 *
 * <p>
 * Keyboard keys are translated to the constants in {@link Keys} transparently on all systems. Do not use system specific key
 * constants.
 * </p>
 *
 * <p>
 * The class also offers methods to use (and test for the presence of) other input systems like vibration, compass, on-screen
 * keyboards, and cursor capture. Support for simple input dialogs is also provided.
 * </p>
  "
  (:require [com.badlogic.gdx.input.buttons :as input-buttons]
            [com.badlogic.gdx.input.keys    :as input-keys])
  (:import (com.badlogic.gdx Input)))

(defn button-just-pressed?
  "Returns whether a given button has just been pressed. Button keys can be found in [input_buttons.md]. On Android only the `:left` button is meaningful before version 4.0."
  [^Input this button]
  {:pre [(contains? input-buttons/k->value button)]}
  (.isButtonJustPressed this (input-buttons/k->value button)))

(defn key-pressed?
  "Returns whether the key is pressed."
  [^Input this key]
  (assert (contains? input-keys/keyword->value key)
          (str "(pr-str key): "(pr-str key)))
  (.isKeyPressed this (input-keys/keyword->value key)))

(defn key-just-pressed?
  "Returns whether the key has just been pressed."
  [^Input this key]
  {:pre [(contains? input-keys/keyword->value key)]}
  (.isKeyJustPressed this (input-keys/keyword->value key)))

(defn set-processor!
  "Sets the {@link InputProcessor} that will receive all touch and key input events. It will be called before the
	 * {@link ApplicationListener#render()} method each frame."
  [^Input this input-processor]
  (.setInputProcessor this input-processor))

(defn mouse-position
  "The x coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner."
  [^Input this]
  [(.getX this)
   (.getY this)])
