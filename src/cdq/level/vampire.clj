(ns cdq.level.vampire
  (:require [clojure.tiled :as tiled]))

(defn create [_ctx]
  {:tiled-map (tiled/load-tmx-map "maps/vampire.tmx") ; TODO not disposed !
   :start-position [32 71]})
