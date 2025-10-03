(ns cdq.application
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [gdl.application.lwjgl :as application])
  (:gen-class))

(def state (atom nil))

(defn pipeline [ctx pipeline]
  (reduce (fn [ctx [f & args]]
            (apply f ctx args))
          ctx
          pipeline))

(defn -main []
  (let [app (-> "cdq.application.edn"
                io/resource
                slurp
                edn/read-string)
        req-resolve (fn [sym sym-format]
                      (requiring-resolve (symbol (format sym-format sym))))
        create-pipeline (map #(update % 0 req-resolve "cdq.ctx.create.%s/do!") (:create-pipeline app))
        render-pipeline (map #(update % 0 req-resolve "cdq.ctx.render.%s/do!") (:render-pipeline app))
        dispose (requiring-resolve (:dispose app))
        resize  (requiring-resolve (:resize app))]
    (run! require (:requires app))
    (application/start! (reify application/Listener
                          (create [_ context]
                            (reset! state (pipeline context create-pipeline)))
                          (dispose [_]
                            (dispose @state))
                          (render [_]
                            (swap! state pipeline render-pipeline))
                          (resize [_ width height]
                            (resize @state width height))
                          (pause [_])
                          (resume [_]))
                        (:config app))))
