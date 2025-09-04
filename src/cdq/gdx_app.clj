(ns cdq.gdx-app
  (:require [cdq.application :as application]
            [cdq.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationListener)))

(defn start!
  [{:keys [lwjgl-app-config
           create!
           dispose!
           render!
           resize!]}]
  (lwjgl/start-application! (reify ApplicationListener
                              (create [_]
                                (reset! application/state
                                        (let [[f config] create!]
                                          ((requiring-resolve f) config))))
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
                              (resume [_]))
                            lwjgl-app-config))
