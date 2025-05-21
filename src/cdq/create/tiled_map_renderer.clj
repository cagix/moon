(ns cdq.create.tiled-map-renderer
  (:require [gdl.tiled :as tiled]))

(defn do! [{:keys [ctx/world-unit-scale
                   ctx/batch]}]
  (memoize (fn [tiled-map]
             (tiled/renderer tiled-map
                             world-unit-scale
                             (:java-object batch)))))
