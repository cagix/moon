(ns cdq.world-fns.modules.load-schema-tiled-map
  (:require [clojure.gdx.maps.tiled.tmx :as tmx]))

(defn step [w]
  (assoc w :schema-tiled-map (tmx/load-map "maps/modules.tmx")))
