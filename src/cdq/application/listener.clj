(ns cdq.application.listener
  (:require [clojure.object :as object]))

(defn create
  [{:keys [create
           dispose
           render
           resize
           atom-var]}]
  (let [state @atom-var]
    {:create! (fn [gdx]
                (swap! state (fn [ctx]
                               (object/pipeline {:object (assoc ctx :ctx/gdx gdx)
                                                 :pipeline create}))))
     :dispose! (fn []
                 (swap! state dispose))
     :render! (fn []
                (swap! state render))
     :resize! (fn [width height]
                (swap! state resize width height))
     :pause! (fn [])
     :resume! (fn [])}))
