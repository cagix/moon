(ns cdq.level.vampire
  (:require [cdq.tiled :as tiled]))

(defn create []
  {:tiled-map (tiled/load-map "maps/vampire.tmx") ; TODO not disposed !
   :start-position [32 71]})
