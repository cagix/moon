(ns anvil.world.tick.camera-controls
  (:require [anvil.controls :as controls]
            [anvil.world.tick :as tick]
            [gdl.graphics :as g]
            [gdl.utils :refer [defn-impl]]))

(defn-impl tick/camera-controls []
  (controls/adjust-zoom g/camera))
