(ns cdq.entity.modifiers
  (:require [cdq.op :as op]
            [clojure.string :as str]))

(defn info-text [[_ mods] _ctx]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op/info-text ops k)) mods))))
