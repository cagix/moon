(ns cdq.application-listener
  (:require [cdq.application :as application])
  (:import (com.badlogic.gdx ApplicationListener)))

(defn create
  [{:keys [create!
           dispose!
           render!
           resize!]}]
  (reify ApplicationListener
    (create [_]
      (reset! application/state
              (let [[f params] create!]
                ((requiring-resolve f) params))))
    (dispose [_]
      ((requiring-resolve dispose!) @application/state))
    (render [_]
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
    (resize [_ width height]
      ((requiring-resolve resize!) @application/state width height))
    (pause [_])
    (resume [_])))
