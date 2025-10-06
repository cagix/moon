(ns cdq.application
  (:require [clojure.core-ext :refer [extend-by-ns]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl]
            [com.badlogic.gdx.backends.lwjgl3.application :as application])
  (:gen-class))

(require 'cdq.graphics.impl) ; for the record class

(extend-by-ns
 '[
   [com.badlogic.gdx.audio.Sound
    com.badlogic.gdx.audio.sound
    clojure.gdx.audio/Sound]

   [com.badlogic.gdx.Files
    clojure.gdx.files
    clojure.files/Files]

   [com.badlogic.gdx.files.FileHandle
    clojure.gdx.files.file-handle
    clojure.files.file-handle/FileHandle]

   [com.badlogic.gdx.Graphics
    clojure.gdx.graphics
    clojure.graphics/Graphics]

   [com.badlogic.gdx.graphics.g2d.Batch
    clojure.gdx.graphics.g2d.batch
    clojure.graphics.batch/Batch]

   [com.badlogic.gdx.graphics.g2d.BitmapFont
    clojure.gdx.graphics.g2d.bitmap-font
    clojure.graphics.bitmap-font/BitmapFont]

   [com.badlogic.gdx.utils.Disposable
    clojure.gdx.utils.disposable
    clojure.disposable/Disposable]

   [cdq.graphics.impl.Graphics
    cdq.graphics.impl.camera
    cdq.graphics.camera/Camera]

   [cdq.graphics.impl.Graphics
    cdq.graphics.impl.disposable
    clojure.disposable/Disposable]

   [cdq.graphics.impl.Graphics
    cdq.graphics.impl.draws
    cdq.graphics.draws/Draws]
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
    (application/create {:create (fn [gdx]
                                   (reset! state (pipeline {:ctx/gdx gdx}
                                                           create-pipeline)))
                         :dispose (fn []
                                    (dispose @state))
                         :render (fn []
                                   (swap! state pipeline render-pipeline))
                         :resize (fn [width height]
                                   (resize @state width height))
                         :pause (fn [])
                         :resume (fn [])}
                        (:config app))))
