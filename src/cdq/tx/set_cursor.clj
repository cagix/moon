(ns cdq.tx.set-cursor
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [gdl.graphics :as graphics]
            [cdq.utils :as utils]))

(defmethod do! :tx/set-cursor [[_ cursor-key]
                               {:keys [ctx/graphics]}]
  (graphics/set-cursor! graphics cursor-key)
  nil)
