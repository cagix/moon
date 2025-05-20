(ns cdq.application.create.cursors
  (:require [cdq.utils :refer [mapvals]]
            [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/config]}]
  (mapvals
   (fn [[file [hotspot-x hotspot-y]]]
     (graphics/cursor (format (:cursor-path-format config) file)
                      hotspot-x
                      hotspot-y))
   (:cursors config)))
