(ns cdq.skill.cooldown
  (:require [gdl.utils :as utils]))

(defn info-text [[_ v] _world]
  (when-not (zero? v)
    (str "Cooldown: " (utils/readable-number v) " seconds")))
