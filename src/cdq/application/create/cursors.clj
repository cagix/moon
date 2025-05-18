(ns cdq.application.create.cursors
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root
                               mapvals]]
            [gdl.graphics :as graphics]))

(defn do! []
  (bind-root #'ctx/cursors (mapvals
                            (fn [[file [hotspot-x hotspot-y]]]
                              (graphics/cursor (format (:cursor-path-format ctx/config) file)
                                               hotspot-x
                                               hotspot-y))
                            (:cursors ctx/config))))
