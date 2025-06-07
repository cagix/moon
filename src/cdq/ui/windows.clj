(ns cdq.ui.windows
  (:require [gdl.ui :as ui]))

(defn create [ctx {:keys [id actors]}]
  (let [group (ui/group {:id id})]
    (doseq [[create-actor params] actors]
      (ui/add! group (create-actor ctx params)))
    group))
