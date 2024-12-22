(ns anvil.entity.mana
  (:refer-clojure :exclude [val])
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.entity.modifiers :as mods]
            [anvil.info :as info]
            [gdl.utils :refer [defn-impl defmethods]]))

(defn-impl entity/mana [entity]
  (-> entity
      :entity/mana
      (mods/apply-max-modifier entity :modifier/mana-max)))

(defn-impl entity/mana-val [entity]
  (if (:entity/mana entity)
    ((entity/mana entity) 0)
    0))

(defn-impl entity/pay-mana-cost [entity cost]
  (let [mana-val ((entity/mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))

(defmethods :entity/mana
  (component/info [_]
    (str "Mana: " (entity/mana info/*info-text-entity*)))

  (component/->v [[_ v]]
    [v v]))
