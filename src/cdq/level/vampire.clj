(ns cdq.level.vampire
  (:require [gdl.gdx :as gdx]))

(defn create [_ctx]
  {:tiled-map (gdx/tmx-tiled-map "maps/vampire.tmx") ; TODO not disposed !
   :start-position [32 71]})
