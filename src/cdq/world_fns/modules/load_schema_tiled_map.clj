(ns cdq.world-fns.modules.load-schema-tiled-map
  (:require [gdl.impl.tiled :as tiled]))

(defn do! [w]
  (assoc w :schema-tiled-map (tiled/tmx-tiled-map "maps/modules.tmx")))
