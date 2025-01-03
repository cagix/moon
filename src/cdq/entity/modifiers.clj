(ns cdq.entity.modifiers
  (:require [clojure.string :as str]
            [gdl.operation :as op]))

(defn info [[_ mods] _c]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op/info ops k)) mods))))
