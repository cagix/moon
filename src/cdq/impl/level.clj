(ns cdq.impl.level
  (:require [cdq.create.level :refer [generate-level*]]
            [cdq.tiled :as tiled]))

(defmethod generate-level* :world.generator/tiled-map [world c]
  {:tiled-map (tiled/load-map (:world/tiled-map world))
   :start-position [32 71]})
