(ns moon.modifiers
  (:refer-clojure :exclude [remove])
  (:require [clojure.math :as math]
            [clojure.string :as str]
            [gdl.utils :refer [safe-remove-one update-kv k->pretty-name]]
            [moon.operation :as op]))

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

(defn add    [mods value-mods] (update-kv ops-add    mods value-mods))
(defn remove [mods value-mods] (update-kv ops-remove mods value-mods))

(defn- sum-ops [ops]
  (for [[k values] ops
        :let [value (apply + values)]
        :when (not (zero? value))]
    [k value]))

(= (sum-ops {:op/inc [1 2 3 -10 ]
             :op/mult [-0.1 3 -0.5]})
   [[:op/inc -4] [:op/mult 2.4]])

(defn sum-operation-values [modifiers]
  (for [[k ops] modifiers
        :let [ops (sum-ops ops)]
        :when (seq ops)]
    [k ops]))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn op-info-text [{value 1 :as operation}]
  (str (+? value) (op/value-text operation)))

; TODO here also do the right sorting like @ modified-value
; TODO can even make this testable ? its the 'functional - core'  ?
(defn info-text [modifiers]
  ; assert summed
  (str "[MODIFIER_BLUE]"
       (str/join "\n"
                 (for [[modifier-k operations] modifiers
                       operation operations]
                   (str (op-info-text operation) " " (k->pretty-name modifier-k))))
       "[]"))
