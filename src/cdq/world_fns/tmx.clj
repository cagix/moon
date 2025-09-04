(ns cdq.world-fns.tmx
  (:require [cdq.gdx.tiled :as tiled]))

(defn create [_ctx {:keys [tmx-file start-position]}]
  {:tiled-map (tiled/tmx-tiled-map tmx-file)
   :start-position start-position})
