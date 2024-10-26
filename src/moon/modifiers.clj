(ns moon.modifiers
  (:refer-clojure :exclude [remove])
  (:require [clojure.string :as str]
            [gdl.utils :refer [update-kv mapvals]]
            [moon.operations :as ops]))

(defn add    [mods value-mods] (update-kv ops/add    mods value-mods))
(defn remove [mods value-mods] (update-kv ops/remove mods value-mods))

(defn sum-vals [mods]
  (for [[k ops] mods
        :let [ops (ops/sum-vals ops)]
        :when (seq ops)]
    [k ops]))

(defn info-text [value-mods]
  (when (seq value-mods)
    (str "[MODIFIER_BLUE]"
         (str/join "\n"
                   (for [[k ops] value-mods]
                     (ops/info-text ops k)))
         "[]")))

(defn value-mods->mods [value-mods]
  (into {} (for [[k ops] value-mods]
             [k (into {} (for [[op-k v] ops]
                           [op-k [v]]))])))
