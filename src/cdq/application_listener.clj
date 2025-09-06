(ns cdq.application-listener
  (:require [cdq.application :as application]
            [cdq.game-record :as game-record]))

(defn- render!* [ctx f]
  (let [result (if (vector? f)
                 (let [[f params] f]
                   ((requiring-resolve f) ctx params))
                 ((requiring-resolve f) ctx))]
    (if (nil? result)
      ctx
      result)))

(defn create
  [{:keys [config
           create!
           dispose!
           render!
           resize!]}]
  {:create! (fn []
              (reset! application/state
                      (reduce render!*
                              (-> (game-record/create-with-schema)
                                  (assoc :ctx/config config))
                              create!)))
   :dispose! (fn []
               ((requiring-resolve dispose!) @application/state))
   :render! (fn []
              (swap! application/state (fn [ctx]
                                         (reduce render!*
                                                 ctx
                                                 render!))))
   :resize! (fn [width height]
              ((requiring-resolve resize!) @application/state width height))
   :pause! (fn [])
   :resume! (fn [])})
