(ns cdq.info.entity.delete-after-duration
  (:require [cdq.timer :as timer]
            [cdq.utils :as utils]))

(defn info-segment [[_ counter] {:keys [ctx/world]}]
  (str "Remaining: " (utils/readable-number (timer/ratio (:world/elapsed-time world) counter)) "/1"))
