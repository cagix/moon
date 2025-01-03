(ns cdq.entity.modifiers
  (:require [clojure.string :as str]
            [cdq.operation :as op]))

(defn info [[_ mods] _entity _c]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op/info ops k)) mods))))
