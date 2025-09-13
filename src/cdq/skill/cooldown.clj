(ns cdq.skill.cooldown
  (:require [clojure.utils :as utils]))

(defn info-text [[_ v] _ctx]
  (when-not (zero? v)
    (str "Cooldown: " (utils/readable-number v) " seconds")))
