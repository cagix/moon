(ns cdq.info.entity.temp-modifier
  (:require [cdq.timer :as timer]
            [cdq.utils :as utils]))

(defn info-segment [[_ {:keys [counter]}] {:keys [ctx/world]}]
  (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio (:world/elapsed-time world) counter)) "/1"))
