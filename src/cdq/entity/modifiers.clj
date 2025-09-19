(ns cdq.entity.modifiers
  (:require [cdq.stats.ops :as ops]
            [clojure.string :as str]))

(defn info-text [[_ mods] _ctx]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (ops/info-text ops k)) mods))))
