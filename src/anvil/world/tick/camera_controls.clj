(ns anvil.world.tick.camera-controls
  (:require [clojure.gdx :refer [key-pressed?]]
            [anvil.world.tick :as tick]
            [gdl.graphics.camera :as cam]))

(def ^:private zoom-speed 0.025)

(defn adjust-zoom [c camera]
  (when (key-pressed? c :minus)  (cam/inc-zoom camera    zoom-speed))
  (when (key-pressed? c :equals) (cam/inc-zoom camera (- zoom-speed))))

(defn-impl tick/camera-controls [c camera]
  (adjust-zoom c camera))
