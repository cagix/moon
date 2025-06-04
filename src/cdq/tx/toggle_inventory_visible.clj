(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [clojure.gdx.ui :as ui]))

(defmethod do! :tx/toggle-inventory-visible [_ {:keys [ctx/stage]}]
  (-> stage :windows :inventory-window ui/toggle-visible!)
  nil)
