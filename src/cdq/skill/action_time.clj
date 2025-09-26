(ns cdq.skill.action-time
  (:require [gdl.utils :as utils]))

(defn info-text [[_ v] _world]
  (str "Action-Time: " (utils/readable-number v) " seconds"))
