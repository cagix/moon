(ns clojure.render.set-camera-on-player
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.entity :as entity]))

(defn do! [{:keys [ctx/player-eid
                   ctx/world-viewport]
            :as ctx}]
  (camera/set-position! (:camera world-viewport)
                        (entity/position @player-eid))
  ctx)
