(ns gdl.graphics
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.screen :as screen]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.graphics Colors)))

(defn clear-screen [context]
  (screen/clear color/black)
  context)

(defn draw-stage [{:keys [gdl.context/stage] :as context}]
  (ui/draw stage (assoc context :gdl.context/unit-scale 1))
  context)

(defn delta-time
  "The time span between the current frame and the last frame in seconds."
  [graphics]
  (com.badlogic.gdx.Graphics/.getDeltaTime graphics))

(defn frames-per-second
  "The average number of frames per second."
  [graphics]
  (com.badlogic.gdx.Graphics/.getFramesPerSecond graphics))

(defn def-color
  "A general purpose class containing named colors that can be changed at will. For example, the markup language defined by the BitmapFontCache class uses this class to retrieve colors and the user can define his own colors.

  Convenience method to add a color with its name. The invocation of this method is equivalent to the expression Colors.getColors().put(name, color)

  Parameters:
  name - the name of the color
  color - the color
  Returns:
  the previous color associated with name, or null if there was no mapping for name ."
  [name-str color]
  (Colors/put name-str color))
