(ns cdq.application.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start!
  [{:keys [ctx/application-state
           ctx/lwjgl
           ctx/create-pipeline
           ctx/render-pipeline
           ctx/dispose-fn
           ctx/resize-fn]
    :as ctx}]
  (lwjgl/start-application!
   {:create! (fn []
               (reset! application-state (reduce (fn [ctx f]
                                                   (let [result (f ctx)]
                                                     (if (nil? result)
                                                       ctx
                                                       result)))
                                                 ctx
                                                 (map requiring-resolve create-pipeline))))
    :dispose! (fn []
                ((requiring-resolve dispose-fn) @application-state))
    :render! (fn []
               (swap! application-state (fn [ctx]
                                          (reduce (fn [ctx f]
                                                    (if-let [new-ctx (f ctx)]
                                                      new-ctx
                                                      ctx))
                                                  ctx
                                                  (map requiring-resolve render-pipeline)))))
    :resize! (fn [width height]
               ((requiring-resolve resize-fn) @application-state width height))
    :pause! (fn [])
    :resume! (fn [])}
   lwjgl))
