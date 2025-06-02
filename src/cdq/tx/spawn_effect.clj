(ns cdq.tx.spawn-effect
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/spawn-effect
  [[_ position components]
   {:keys [ctx/config]
    :as ctx}]
  (ctx/spawn-entity! ctx
                     position
                     (:effect-body-props config)
                     components))
