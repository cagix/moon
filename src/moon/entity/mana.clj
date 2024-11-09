(ns moon.entity.mana
  (:require [moon.component :as component]
            [moon.val-max :as val-max]))

(defn value
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :entity/mana
      (val-max/apply-max-modifier entity :modifier/mana-max)))

(defn info [_]
  (str "Mana: " (value component/*info-text-entity*)))

(defn ->v [v]
  [v v])

; TODO same as hp @ damage ... and similar to effect.entity/mana or hp ...
(defmethod component/handle :tx/pay-mana-cost [[_ eid cost]]
  (let [mana-val ((value @eid) 0)]
    (assert (<= cost mana-val))
    (swap! eid assoc-in [:entity/mana 0] (- mana-val cost))
    nil))
