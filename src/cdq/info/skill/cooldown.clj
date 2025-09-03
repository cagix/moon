(ns cdq.info.skill.cooldown
  (:require [cdq.utils :as utils]))

(defn info-segment [[_ v] _ctx]
  (when-not (zero? v)
    (str "Cooldown: " (utils/readable-number v) " seconds")))
