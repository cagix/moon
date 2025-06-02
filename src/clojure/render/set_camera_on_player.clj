(ns clojure.render.set-camera-on-player
  (:require [clojure.entity :as entity]
            [clojure.graphics.camera :as camera]))

(defn do! [{:keys [ctx/player-eid
                   ctx/world-viewport]
            :as ctx}]
  (camera/set-position! (:camera world-viewport)
                        (entity/position @player-eid))
  ctx)
