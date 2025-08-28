(ns cdq.level.from-tmx
  (:require [cdq.tiled :as tiled]))

(defn create [_ctx {:keys [tmx-file start-position]}]
  {:tiled-map (tiled/tmx-tiled-map tmx-file)
   :start-position start-position})
