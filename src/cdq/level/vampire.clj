(ns cdq.level.vampire
  (:require [gdl.tiled :as tiled]))

(defn create [_ctx]
  {:tiled-map (tiled/tmx-tiled-map "maps/vampire.tmx") ; TODO not disposed !
   :start-position [32 71]})
