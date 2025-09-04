(ns cdq.info.entity.delete-after-duration
  (:require [cdq.timer :as timer]
            [cdq.utils :as utils]))

(defn info-segment [[_ counter] {:keys [ctx/elapsed-time]}]
  (str "Remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
