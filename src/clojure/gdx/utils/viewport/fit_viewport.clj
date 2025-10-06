(ns clojure.gdx.utils.viewport.fit-viewport
  (:require [clojure.graphics.viewport]
            [com.badlogic.gdx.math.vector2 :as vector2]
            [com.badlogic.gdx.utils.viewport :as viewport]
            [com.badlogic.gdx.utils.viewport.fit-viewport :as fit-viewport]))

(defn create [width height camera]
  (fit-viewport/create width height camera))

(extend com.badlogic.gdx.utils.viewport.Viewport
  clojure.graphics.viewport/Viewport
  {:camera       viewport/camera
   :world-width  viewport/world-width
   :world-height viewport/world-height
   :update!      viewport/update!
   :unproject    (fn [viewport [x y]]
                   (-> viewport
                       (viewport/unproject (vector2/->java x y))
                       vector2/->clj))})
