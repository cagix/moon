(ns gdl.application.lwjgl
  (:require [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.configuration :as config])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (org.lwjgl.system Configuration)))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height])
  (pause [_])
  (resume [_]))

(defn- extend-types [impls]
  (doseq [[atype-sym implementation-ns-sym protocol-sym] impls]
    (try (let [atype (eval atype-sym)
               _ (assert (class atype))
               protocol-var (requiring-resolve protocol-sym)
               protocol @protocol-var
               method-map (update-vals (:sigs protocol)
                                       (fn [{:keys [name]}]
                                         (requiring-resolve (symbol (str implementation-ns-sym "/" name)))))]
           (extend atype protocol method-map))
         (catch Throwable t
           (throw (ex-info "Cant extend"
                           {:atype-sym atype-sym
                            :implementation-ns-sym implementation-ns-sym
                            :protocol-sym protocol-sym}
                           t))))))

(defn start! [listener config]
  (extend-types
   [
    ['com.badlogic.gdx.Audio
     'com.badlogic.gdx.audio
     'gdl.audio/Audio]
    ['com.badlogic.gdx.audio.Sound
     'com.badlogic.gdx.audio.sound
     'gdl.audio.sound/Sound]
    ['com.badlogic.gdx.Files
     'com.badlogic.gdx.files
     'gdl.files/Files]
    ['com.badlogic.gdx.graphics.OrthographicCamera
     'com.badlogic.gdx.graphics.orthographic-camera
     'gdl.graphics.orthographic-camera/Camera]
    ['com.badlogic.gdx.utils.viewport.Viewport
     'com.badlogic.gdx.utils.viewport
     'gdl.graphics.viewport/Viewport]
    ['com.badlogic.gdx.files.FileHandle
     'com.badlogic.gdx.files.file-handle
     'gdl.files.file-handle/FileHandle]
    ['com.badlogic.gdx.utils.Disposable
     'com.badlogic.gdx.utils.disposable
     'gdl.disposable/Disposable]
    ['com.badlogic.gdx.scenes.scene2d.Actor
     'com.badlogic.gdx.scenes.scene2d.actor
     'gdl.scene2d.actor/Actor]
    ['com.badlogic.gdx.scenes.scene2d.Actor
     'clojure.scene2d.tooltip
     'gdl.scene2d.actor/Tooltip]
    ]
   )
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (application/start! (reify ApplicationListener
                        (create [_]
                          (let [state {:ctx/audio    Gdx/audio
                                       :ctx/files    Gdx/files
                                       :ctx/graphics Gdx/graphics
                                       :ctx/input    Gdx/input}]
                            (create listener state)))
                        (dispose [_]
                          (dispose listener))
                        (render [_]
                          (render listener))
                        (resize [_ width height]
                          (resize listener width height))
                        (pause [_]
                          (pause listener))
                        (resume [_]
                          (resume listener)))
                      (config/create config)))
