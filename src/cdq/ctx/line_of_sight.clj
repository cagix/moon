(ns cdq.ctx.line-of-sight
  (:require [cdq.entity :as entity]
            [cdq.raycaster :as raycaster]))

(defn line-of-sight? [{:keys [ctx/raycaster]}
                      source
                      target]
  (not (raycaster/blocked? raycaster
                           (entity/position source)
                           (entity/position target))))
