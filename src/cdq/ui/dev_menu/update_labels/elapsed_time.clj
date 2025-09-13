(ns cdq.ui.dev-menu.update-labels.elapsed-time
  (:require [clojure.utils :as utils]))

(defn create [icon]
  {:label "elapsed-time"
   :update-fn (fn [ctx]
                (str (utils/readable-number (:ctx/elapsed-time ctx)) " seconds"))
   :icon icon})
