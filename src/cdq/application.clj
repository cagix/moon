(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.application :as application])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)
        create-pipeline (map requiring-resolve (:create config))
        dispose (requiring-resolve (:dispose config))
        render-pipeline (map requiring-resolve (:render config))
        resize (requiring-resolve (:resize config))]
    (application/start!
     (assoc config
            :create (fn [context]
                      (reset! state (reduce (fn [ctx f]
                                              (f ctx))
                                            context
                                            create-pipeline)))
            :dispose (fn []
                       (dispose @state))
            :render (fn []
                      (swap! state (fn [ctx]
                                     (reduce (fn [ctx f]
                                               (f ctx))
                                             ctx
                                             render-pipeline))))
            :resize (fn [ width height]
                      (resize @state width height))))))
