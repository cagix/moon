(ns cdq.ui.dev-menu.update-labels.elapsed-time
  (:require [cdq.utils :as utils]))

(defn create [icon]
  {:label "elapsed-time"
   :update-fn (fn [ctx]
                (str (utils/readable-number (:ctx/elapsed-time ctx)) " seconds"))
   :icon icon})
