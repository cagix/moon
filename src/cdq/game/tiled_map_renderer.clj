(ns cdq.game.tiled-map-renderer
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [gdl.tiled :as tiled]))

(defn do! []
  (utils/bind-root #'ctx/get-tiled-map-renderer
                   (memoize (fn [tiled-map]
                              (tiled/renderer tiled-map
                                              ctx/world-unit-scale
                                              (:java-object ctx/batch))))))
