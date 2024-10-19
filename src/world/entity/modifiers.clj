(ns world.entity.modifiers
  (:require [clojure.string :as str]
            [component.core :refer [defc]]
            [component.info :as info]
            [component.operation :as op]
            [component.tx :as tx]
            [gdx.graphics :as g]
            [utils.core :refer [remove-one k->pretty-name]]
            [world.entity :as entity]))

(defn- txs-update-modifiers [eid modifiers f]
  (for [[modifier-k operations] modifiers
        [operation-k value] operations]
    [:e/update-in eid [:entity/modifiers modifier-k operation-k] (f value)]))

(defn- conj-value [value]
  (fn [values]
    (conj values value)))

(defn- remove-value [value]
  (fn [values]
    {:post [(= (count %) (dec (count values)))]}
    (remove-one values value)))

(defc :tx/apply-modifiers
  (tx/do! [[_ eid modifiers]]
    (txs-update-modifiers eid modifiers conj-value)))

(defc :tx/reverse-modifiers
  (tx/do! [[_ eid modifiers]]
    (txs-update-modifiers eid modifiers remove-value)))

; DRY ->effective-value (summing)
; also: sort-by op/order @ modifier/info-text itself (so player will see applied order)
(defn- sum-operation-values [modifiers]
  (for [[modifier-k operations] modifiers
        :let [operations (for [[operation-k values] operations
                               :let [value (apply + values)]
                               :when (not (zero? value))]
                           [operation-k value])]
        :when (seq operations)]
    [modifier-k operations]))

(g/def-markup-color "MODIFIER_BLUE" :cyan)

; For now no green/red color for positive/negative numbers
; as :stats/damage-receive negative value would be red but actually a useful buff
; -> could give damage reduce 10% like in diablo 2
; and then make it negative .... @ applicator
(def ^:private positive-modifier-color "[MODIFIER_BLUE]" #_"[LIME]")
(def ^:private negative-modifier-color "[MODIFIER_BLUE]" #_"[SCARLET]")

(defn mod-info-text [modifiers]
  (str "[MODIFIER_BLUE]"
       (str/join "\n"
                 (for [[modifier-k operations] modifiers
                       operation operations]
                   (str (op/info-text operation) " " (k->pretty-name modifier-k))))
       "[]"))

(defc :entity/modifiers
  {:schema [:s/components-ns :modifier]
   :let modifiers}
  (entity/->v [_]
    (into {} (for [[modifier-k operations] modifiers]
               [modifier-k (into {} (for [[operation-k value] operations]
                                      [operation-k [value]]))])))

  (info/text [_]
    (let [modifiers (sum-operation-values modifiers)]
      (when (seq modifiers)
        (mod-info-text modifiers)))))

(defn ->modified-value [{:keys [entity/modifiers]} modifier-k base-value]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (->> modifiers
       modifier-k
       (sort-by op/order)
       (reduce (fn [base-value [operation-k values]]
                 (op/apply [operation-k (apply + values)] base-value))
               base-value)))
