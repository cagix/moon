(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl])
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
    (lwjgl/set-glfw-library-name! "glfw_async")
    (application/start! {:create (fn [context]
                                   (reset! state (pipeline context create-pipeline)))
                         :dispose (fn []
                                    (dispose @state))
                         :render (fn []
                                   (swap! state pipeline render-pipeline))
                         :resize (fn [width height]
                                   (resize @state width height))
                         :pause (fn [])
                         :resume (fn [])}
                        (:config app))))
