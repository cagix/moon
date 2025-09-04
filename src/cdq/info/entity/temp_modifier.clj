(ns cdq.info.entity.temp-modifier
  (:require [cdq.timer :as timer]
            [cdq.utils :as utils]))

(defn info-segment [[_ {:keys [counter]}] {:keys [ctx/elapsed-time]}]
  (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
