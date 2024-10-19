(ns data.modifiers
  ; what is modifiers?
  ; what is operations?
  ; ops map of [op value]
  ; o=modifiers map of [mod/k ops]
  ; one modifier - [:mod/k ops]
  (:require [data.ops :as ops]
            [utils.core :refer [safe-remove-one update-kv mapvals]]))

; ops are map of :op/k to coll of values
; value-ops are map of :op/k to value (from some modifiers being added to existing)

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

(defn- sum-values [modifiers]
  (for [[modifier-k operations] modifiers
        :let [operations (for [[operation-k values] operations
                               :let [value (apply + values)]
                               :when (not (zero? value))]
                           [operation-k value])]
        :when (seq operations)]
    [modifier-k operations]))

(defn- mod-info-text [modifiers]
  (str "[MODIFIER_COLOR]"
       (str/join "\n"
                 (for [[modifier-k operations] modifiers
                       operation operations]
                   (str (op/info-text operation) " " (k->pretty-name modifier-k))))
       "[]"))

(defn info-text [modifiers]
  (let [modifiers (sum-operation-values modifiers)]
      (when (seq modifiers)
        (mod-info-text modifiers))))

(defn coll-mods [value-mods]
  (mapvals #(mapvals list %) value-mods))

(comment
 (= (coll-mods
     {:modifier/damage-deal {:op/val-inc 30
                             :op/val-mult 0.5}})
    #:modifier{:damage-deal #:op{:val-inc [30], :val-mult [0.5]}})
 )

(comment
 (= (info/text [:entity/modifiers
                {:modifier/damage-deal {:op/val-inc [30]
                                        :op/val-mult [0.5]}}])
    "[MODIFIER_BLUE]+30 Minimum Damage-deal\n+50% Minimum Damage-deal[]")
 )


; TODO summing & sorting do same @ info-text
; & stat-effects
; also fn for apply
; => modifiers/apply ! ?
(defn apply-ops [operations base-value]
  (->> operations
       (sort-by op/order)
       (reduce (fn [base-value [operation-k values]]
                 (op/apply [operation-k (apply + values)] base-value))
               base-value)))
