(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io])
  (:gen-class))

(defn- execute! [[f params]]
  ((requiring-resolve f) params))

(defn- create-listener
  [{:keys [state
           initial-record
           create-pipeline
           dispose-fn
           render-pipeline
           resize-fn]}]
  (let [state @(requiring-resolve state)
        create! (fn []
                  (reduce (fn [ctx f]
                            (let [result (if (vector? f)
                                           (let [[f params] f]
                                             ((requiring-resolve f) ctx params))
                                           ((requiring-resolve f) ctx))]
                              (if (nil? result)
                                ctx
                                result)))
                          ((requiring-resolve initial-record))
                          create-pipeline))
        dispose! (fn [ctx]
                   (requiring-resolve dispose-fn) ctx)
        render! (fn [ctx]
                  (reduce (fn [ctx f]
                            (if-let [new-ctx ((requiring-resolve f) ctx)]
                              new-ctx
                              ctx))
                          ctx
                          render-pipeline))
        resize! (fn [ctx width height]
                  ((requiring-resolve resize-fn) ctx width height))]
    {:create! (fn []
                (reset! state (create!)))
     :dispose! (fn []
                 (dispose! @state))
     :render! (fn []
                (swap! state render!))
     :resize! (fn [width height]
                (resize! @state width height))
     :pause! (fn [])
     :resume! (fn [])}))

(defn -main []
  (let [{:keys [operating-sytem->executables
                listener
                config]} (-> "cdq.start.edn" io/resource slurp edn/read-string)]
    (->> (shared-library-loader/operating-system)
         operating-sytem->executables
         (run! execute!))
    (lwjgl/start-application! (create-listener listener)
                              config)))
