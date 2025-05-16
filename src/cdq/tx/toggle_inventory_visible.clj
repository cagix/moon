(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ctx :as ctx]
            [clojure.ui.actor :as actor]))

(defn do! []
  (-> ctx/stage
      :windows
      :inventory-window
      actor/toggle-visible!))
