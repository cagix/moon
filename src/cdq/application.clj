(ns cdq.application
  (:require [clojure.core-ext :refer [extend-by-ns]]
            [clojure.edn :as edn]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.configuration :as config]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl])
  (:import (com.badlogic.gdx Gdx))
  (:gen-class))

(extend-by-ns
 '[
   [com.badlogic.gdx.Audio
    clojure.gdx.audio
    clojure.audio/Audio]
   [com.badlogic.gdx.audio.Sound
    clojure.gdx.audio.sound
    clojure.audio.sound/Sound]
   [com.badlogic.gdx.Files
    clojure.gdx.files
    clojure.files/Files]
   [com.badlogic.gdx.files.FileHandle
    clojure.gdx.files.file-handle
    clojure.files.file-handle/FileHandle]
   ]
 )

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
    (application/create (listener/create
                         {:create (fn []
                                    (reset! state (pipeline {:ctx/audio    Gdx/audio
                                                             :ctx/files    Gdx/files
                                                             :ctx/graphics Gdx/graphics
                                                             :ctx/input    Gdx/input}
                                                            create-pipeline)))
                          :dispose (fn []
                                     (dispose @state))
                          :render (fn []
                                    (swap! state pipeline render-pipeline))
                          :resize (fn [width height]
                                    (resize @state width height))
                          :pause (fn [])
                          :resume (fn [])})
                        (config/create (:config app)))))
