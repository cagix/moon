(ns cdq.tx.toggle-inventory-visible
  (:require [clojure.ctx.effect-handler :refer [do!]]
            [gdl.ui :as ui]))

(defmethod do! :tx/toggle-inventory-visible [_ {:keys [ctx/stage]}]
  (-> stage :windows :inventory-window ui/toggle-visible!))
