(ns gdl.application.lwjgl
  (:require gdl.audio
            gdl.audio.sound
            gdl.files
            gdl.files.file-handle
            gdl.disposable)
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.utils Disposable)
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
    ['com.badlogic.gdx.scenes.scene2d.Actor
     'clojure.scene2d.tooltip
     'gdl.scene2d.actor/Tooltip]
    ]
   )
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
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
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setWindowedMode (:width (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setTitle (:title config))
                        (.setForegroundFPS (:foreground-fps config)))))


(extend-type Audio
  gdl.audio/Audio
  (new-sound [this file-handle]
    (.newSound this file-handle)))

(extend-type Sound
  gdl.audio.sound/Sound
  (play! [this]
    (.play this)))

(extend-type Files
  gdl.files/Files
  (internal [this path]
    (.internal this path)))

(extend FileHandle
  gdl.files.file-handle/FileHandle
  {:list       FileHandle/.list
   :directory? FileHandle/.isDirectory
   :extension  FileHandle/.extension
   :path       FileHandle/.path})

(extend Disposable
  gdl.disposable/Disposable
  {:dispose! Disposable/.dispose})
