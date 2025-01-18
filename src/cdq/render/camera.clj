(ns cdq.render.camera
  (:require [clojure.graphics.camera :as camera]))

(defn set-on-player
  [{:keys [clojure.graphics/world-viewport
           clojure.context/player-eid]
    :as context}]
  {:pre [world-viewport
         player-eid]}
  (camera/set-position (:camera world-viewport)
                       (:position @player-eid))
  context)
