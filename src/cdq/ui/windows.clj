(ns cdq.ui.windows
  (:require [gdl.ui :as ui]))

(defn create [{:keys [ctx/config] :as ctx} actors]
  (let [group (ui/group {:id :windows})]
    (doseq [[create-actor params] actors]
      (ui/add! group (create-actor ctx params)))
    group))
