(ns cdq.create.world-viewport
  (:require [cdq.graphics.camera :as camera]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(def config {:width 1440 :height 900})

(defn create [world-unit-scale]
  {:pre [world-unit-scale]}
  (let [camera (OrthographicCamera.)
        world-width  (* (:width  config) world-unit-scale)
        world-height (* (:height config) world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (fit-viewport/create world-width world-height camera)))
