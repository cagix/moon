(ns cdq.game.camera-controls
  (:require [cdq.ctx :as ctx]
            [clojure.graphics.camera :as camera]
            [clojure.input :as input]))

(defn do! []
  (when (input/key-pressed? (get ctx/controls :zoom-in))
    (camera/inc-zoom! (:camera ctx/world-viewport) ctx/zoom-speed))
  (when (input/key-pressed? (get ctx/controls :zoom-out))
    (camera/inc-zoom! (:camera ctx/world-viewport) (- ctx/zoom-speed))))
