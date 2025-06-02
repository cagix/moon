(ns cdq.tx.spawn-effect
  (:require [clojure.ctx :as ctx]
            [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/spawn-effect
  [[_ position components]
   {:keys [ctx/config]
    :as ctx}]
  (ctx/spawn-entity! ctx
                     position
                     (:effect-body-props config)
                     components))
