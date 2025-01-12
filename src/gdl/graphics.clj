(ns gdl.graphics
  (:require [clojure.gdx.utils.screen :as screen]
            [gdl.graphics.color :as color]
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
  []
  (.getDeltaTime com.badlogic.gdx.Gdx/graphics))

(defn frames-per-second
  "The average number of frames per second."
  []
  (.getFramesPerSecond com.badlogic.gdx.Gdx/graphics))

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
