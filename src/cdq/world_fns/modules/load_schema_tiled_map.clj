(ns cdq.world-fns.modules.load-schema-tiled-map
  (:require [clojure.gdx.tiled :as tiled]))

(defn do! [w]
  (assoc w :schema-tiled-map (tiled/tmx-tiled-map "maps/modules.tmx")))
