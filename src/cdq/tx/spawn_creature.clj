(ns cdq.tx.spawn-creature
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/spawn-creature [[_ opts] ctx]
  (ctx/spawn-creature! ctx opts))
