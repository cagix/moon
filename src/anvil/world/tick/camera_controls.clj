(ns anvil.world.tick.camera-controls
  (:require [anvil.world.tick :as tick]
            [gdl.graphics.camera :as cam]
            [gdl.utils :refer [defn-impl]]))

(def ^:private zoom-speed 0.025)

(defn adjust-zoom [camera]
  (when (key-pressed? :keys/minus)  (cam/inc-zoom camera    zoom-speed))
  (when (key-pressed? :keys/equals) (cam/inc-zoom camera (- zoom-speed))))

(defn-impl tick/camera-controls [camera]
  (adjust-zoom camera))
