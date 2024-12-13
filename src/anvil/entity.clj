(ns anvil.entity)

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
