(ns world.entity.modifiers
  (:require [clojure.string :as str]
            [component.core :refer [defc]]
            [component.info :as info]
            [component.tx :as tx]
            [data.operation :as op]
            [data.ops :as ops]
            [gdx.graphics :as g]
            [utils.core :refer [safe-remove-one update-kv k->pretty-name]]
            [world.entity :as entity]))

(defn- ops-add    [ops value-ops] (update-kv conj            ops value-ops))
(defn- ops-remove [ops value-ops] (update-kv safe-remove-one ops value-ops))

(comment
 (= (ops-add {:+ [1 2 3]}
             {:* -0.5 :+ -1})
    {:+ [1 2 3 -1], :* [-0.5]})

 (= (ops-remove {:+ [1 2 3] :* [-0.5]}
                {:+ 2 :* -0.5})
    {:+ [1 3], :* []})
 )

(defn- mods-add    [mods value-mods] (update-kv ops-add    mods value-mods))
(defn- mods-remove [mods value-mods] (update-kv ops-remove mods value-mods))

(comment
 (= (mods-add {:speed {:+ [1 2 3]}}
              {:speed {:* -0.5}})
    {:speed {:+ [1 2 3], :* [-0.5]}})

 (= (mods-remove {:speed {:+ [1 2 3] :* [-0.5]}}
                 {:speed {:+ 2 :* -0.5}})
    {:speed {:+ [1 3], :* []}})
 )

(defn update-mods [[_ eid mods] f]
  [[:e/update eid :entity/modifiers #(f % mods)]])

(defc :tx/apply-modifiers   (tx/handle [this] (update-mods this mods-add)))
(defc :tx/reverse-modifiers (tx/handle [this] (update-mods this mods-remove)))

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
