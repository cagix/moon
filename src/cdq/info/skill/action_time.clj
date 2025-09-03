(ns cdq.info.skill.action-time
  (:require [cdq.utils :as utils]))

(defn info-segment [[_ v] _ctx]
  (str "Action-Time: " (utils/readable-number v) " seconds"))
