(ns cdq.skill.action-time
  (:require [cdq.utils :as utils]))

(defn info-text [[_ v] _ctx]
  (str "Action-Time: " (utils/readable-number v) " seconds"))
