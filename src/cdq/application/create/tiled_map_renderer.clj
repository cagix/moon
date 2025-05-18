(ns cdq.application.create.tiled-map-renderer
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.tiled :as tiled]))

(defn do! []
  (bind-root #'ctx/get-tiled-map-renderer (memoize (fn [tiled-map]
                                                     (tiled/renderer tiled-map
                                                                     ctx/world-unit-scale
                                                                     (:java-object ctx/batch))))))
