(ns cdq.tx.spawn-creature
  (:require [clojure.ctx :as ctx]
            [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/spawn-creature [[_ opts] ctx]
  (ctx/spawn-creature! ctx opts))
