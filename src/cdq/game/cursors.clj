(ns cdq.game.cursors
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/cursors (utils/mapvals
                                  (fn [[file [hotspot-x hotspot-y]]]
                                    (graphics/cursor (format ctx/cursor-path-format file)
                                                     hotspot-x
                                                     hotspot-y))
                                  ctx/cursor-config)))
