(ns cdq.ui.windows
  (:require [gdl.ui :as ui]
            [cdq.ui.entity-info-window :as entity-info-window]
            [cdq.widgets.inventory :as inventory]))

(defn create [c]
  (ui/group {:id :windows
             :actors [(entity-info-window/create c)
                      (inventory/create c)]}))
