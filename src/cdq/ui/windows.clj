(ns cdq.ui.windows
  (:require [gdl.ui :as ui]))

(defn create [{:keys [ctx/config] :as ctx}]
  (let [group (ui/group {:id :windows})]
    (doseq [create-actor (::create-actors config)]
      (ui/add! group (create-actor ctx)))
    group))
