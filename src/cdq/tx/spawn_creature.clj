(ns cdq.tx.spawn-creature
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.world :as world]))

(defmethod do! :tx/spawn-creature [[_ opts] ctx]
  (world/spawn-creature! ctx opts))
