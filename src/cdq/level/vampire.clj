(ns cdq.level.vampire
  (:require [clojure.gdx.tiled :as tiled]))

(defn create [_creature-properties]
  {:tiled-map (tiled/load-map "maps/vampire.tmx") ; TODO not disposed !
   :start-position [32 71]})
