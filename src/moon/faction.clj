(ns moon.faction)

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
