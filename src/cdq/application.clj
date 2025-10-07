(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration))
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
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (Lwjgl3Application. (listener/create
                         {:create (fn []
                                    (reset! state (pipeline {:ctx/gdx (gdx/context)}
                                                            create-pipeline)))
                          :dispose (fn []
                                     (dispose @state))
                          :render (fn []
                                    (swap! state pipeline render-pipeline))
                          :resize (fn [width height]
                                    (resize @state width height))
                          :pause (fn [])
                          :resume (fn [])})
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle "Cyber Dungeon Quest")
                          (.setWindowedMode 1440 900)
                          (.setForegroundFPS 60)))))
