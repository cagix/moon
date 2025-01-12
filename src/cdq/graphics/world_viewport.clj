(ns cdq.graphics.world-viewport
  (:require [clojure.graphics.camera :as camera]
            [clojure.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create [{:keys [clojure.graphics/world-unit-scale]} config]
  {:pre [world-unit-scale
         (:width  config)
         (:height config)]}
  (let [camera (OrthographicCamera.)
        world-width  (* (:width  config) world-unit-scale)
        world-height (* (:height config) world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (fit-viewport/create world-width world-height camera)))
