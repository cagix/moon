(ns cdq.application.listener
  (:require [clojure.utils :as utils]))

(defn create
  [{:keys [create
           dispose
           render
           resize
           atom-var]}]
  (let [state @atom-var]
    {:create! (fn []
                (swap! state utils/pipeline create))
     :dispose! (fn []
                 (swap! state dispose))
     :render! (fn []
                (swap! state utils/pipeline render))
     :resize! (fn [width height]
                (swap! state resize width height))
     :pause! (fn [])
     :resume! (fn [])}))
