(ns cdq.start
  (:require cdq.application.listener
            cdq.ctx.listener
            [cdq.core :as core]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main []
  (let [{:keys [operating-sytem->executables
                config
                state-atom]} (-> "cdq.start.edn"
                                 io/resource
                                 slurp
                                 edn/read-string)
        listener (cdq.application.listener/create @(requiring-resolve state-atom)
                                                  (cdq.ctx.listener/create))]
    (->> (shared-library-loader/operating-system)
         operating-sytem->executables
         (run! core/execute!))
    (lwjgl/start-application! listener config)))
