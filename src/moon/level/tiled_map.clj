(ns ^:no-doc moon.level.tiled-map
  (:require [gdl.tiled :as t]
            [moon.level :as level]))

(defmethod level/generate* :world.generator/tiled-map [world]
  {:tiled-map (t/load-map (:world/tiled-map world))
   :start-position [32 71]})

