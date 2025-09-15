(ns cdq.application.listener
  (:require [clojure.object :as object]))

(defn create
  [{:keys [create
           dispose
           render
           resize
           atom-var]}]
  (let [state @atom-var]
    {:create! (fn []
                (swap! state object/pipeline create))
     :dispose! (fn []
                 (swap! state dispose))
     :render! (fn []
                (swap! state (fn [ctx]
                               (reduce (fn [ctx f]
                                         (if-let [new-ctx (f ctx)]
                                           new-ctx
                                           ctx))
                                       ctx
                                       render))))
     :resize! (fn [width height]
                (swap! state resize width height))
     :pause! (fn [])
     :resume! (fn [])}))
