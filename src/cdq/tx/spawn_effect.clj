(ns cdq.tx.spawn-effect
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.world :as world]))

(defmethod do! :tx/spawn-effect
  [[_ position components]
   {:keys [ctx/config]
    :as ctx}]
  (world/spawn-entity! ctx
                       position
                       (:effect-body-props config)
                       components)
  nil)
