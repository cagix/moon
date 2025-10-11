(ns cdq.world-fns.modules.load-schema-tiled-map
  (:require [cdq.tiled :as tiled]))

(defn step [w]
  (assoc w :schema-tiled-map (tiled/tmx-tiled-map "maps/modules.tmx")))
