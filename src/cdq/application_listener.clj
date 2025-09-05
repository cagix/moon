(ns cdq.application-listener
  (:require [cdq.application :as application]))

(defn create
  [{:keys [create!
           dispose!
           render!
           resize!]}]
  {:create! (fn []
              (reset! application/state
                      (let [[f params] create!]
                        ((requiring-resolve f) params))))
   :dispose! (fn []
               ((requiring-resolve dispose!) @application/state))
   :render! (fn []
              (swap! application/state (fn [ctx]
                                         (reduce (fn [ctx f]
                                                   (let [result (if (vector? f)
                                                                  (let [[f params] f]
                                                                    ((requiring-resolve f) ctx params))
                                                                  ((requiring-resolve f) ctx))]
                                                     (if (nil? result)
                                                       ctx
                                                       result)))
                                                 ctx
                                                 render!))))
   :resize! (fn [width height]
              ((requiring-resolve resize!) @application/state width height))
   :pause! (fn [])
   :resume! (fn [])})
