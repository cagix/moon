(ns gdl.graphics
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.utils.screen :as screen]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [gdl.graphics.color :as color]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.graphics Colors)))

(defn- world-viewport [{:keys [width height]} world-unit-scale]
  (assert world-unit-scale)
  (let [camera (orthographic-camera/create)
        world-width  (* width  world-unit-scale)
        world-height (* height world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (fit-viewport/create world-width world-height camera)))

(defrecord Graphics []
  gdl.utils/Resizable
  (resize [this width height]
    ;(println "Resizing ui-viewport.")
    (viewport/resize (:ui-viewport    this) width height :center-camera? true)
    ;(println "Resizing world-viewport.")
    (viewport/resize (:world-viewport this) width height :center-camera? false)))

(defn create [{:keys [gdl.graphics/shape-drawer-texture
                      gdl.graphics/world-unit-scale]}
              config]
  (map->Graphics
   {:ui-viewport (fit-viewport/create (:width  (:ui-viewport config))
                                      (:height (:ui-viewport config))
                                      (orthographic-camera/create))
    :world-viewport (world-viewport (:world-viewport config) world-unit-scale)}))

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
