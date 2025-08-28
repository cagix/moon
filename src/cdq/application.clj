(ns cdq.application
  (:require [cdq.app]))

(def state (atom nil))

(defn post-runnable! [runnable]
  (swap! state cdq.app/add-runnable runnable)
  nil)
