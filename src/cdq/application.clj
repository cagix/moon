(ns cdq.application
  (:require [clojure.gdx]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [org.lwjgl.system.configuration :as lwjgl-system])
  (:import (com.badlogic.gdx ApplicationListener))
  (:gen-class))

(deftype AtomStateApp [state create-pipeline dispose render-pipeline resize]
  ApplicationListener
  (create [_]
    (reset! state (reduce (fn [ctx f]
                            (f ctx))
                          {:ctx/audio    (gdx/audio)
                           :ctx/files    (gdx/files)
                           :ctx/graphics (gdx/graphics)
                           :ctx/input    (gdx/input)}
                          create-pipeline)))
  (dispose [_]
    (dispose @state))
  (render [_]
    (swap! state (fn [ctx]
                   (reduce (fn [ctx f]
                             (f ctx))
                           ctx
                           render-pipeline))))
  (resize [_ width height]
    (resize @state width height))
  (pause [_])
  (resume [_]))

(def state (atom nil))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)
        create-pipeline (map requiring-resolve (:create config))
        dispose (requiring-resolve (:dispose config))
        render-pipeline (map requiring-resolve (:render config))
        resize (requiring-resolve (:resize config))]
    (lwjgl-system/set-glfw-library-name! "glfw_async")
    (lwjgl/application (->AtomStateApp state
                                       create-pipeline
                                       dispose
                                       render-pipeline
                                       resize)
                       config)))
