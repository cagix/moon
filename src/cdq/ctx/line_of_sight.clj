(ns cdq.ctx.line-of-sight
  (:require [cdq.entity :as entity]
            [cdq.raycaster :as raycaster]))

(defn line-of-sight? [{:keys [ctx/world]}
                      source
                      target]
  (not (raycaster/blocked? (:world/raycaster world)
                           (entity/position source)
                           (entity/position target))))
