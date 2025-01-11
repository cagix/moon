(ns cdq.graphics.world-viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create [{:keys [gdl/config
                      gdl.graphics/world-unit-scale] :as context}]
  {:pre [world-unit-scale
         (::width  config)
         (::height config)]}
  (assoc context :gdl.graphics/world-viewport
         (let [camera (OrthographicCamera.)
               world-width  (* (::width  config) world-unit-scale)
               world-height (* (::height config) world-unit-scale)]
           (camera/set-to-ortho camera world-width world-height :y-down? false)
           (fit-viewport/create world-width world-height camera
                                :center-camera? false))))
