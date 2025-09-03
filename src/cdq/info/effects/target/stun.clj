(ns cdq.info.effects.target.stun
  (:require [cdq.utils :as utils]))

(defn info-segment [[_ duration] _ctx]
  (str "Stuns for " (utils/readable-number duration) " seconds"))
