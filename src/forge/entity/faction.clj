(ns forge.entity.faction)

(defn e-enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
