(ns cdq.application
  (:require cdq.application.create
            cdq.application.dispose
            cdq.application.render
            cdq.application.resize
            clojure.gdx.application
            clojure.gdx.backends.lwjgl
            clojure.java.io))

(def state (atom nil))

(def ^:private runnables (atom []))

(defn post-runnable [f]
  (swap! runnables conj f))

(defn -main []
  (.setIconImage (java.awt.Taskbar/getTaskbar)
                 (.getImage (java.awt.Toolkit/getDefaultToolkit)
                            (clojure.java.io/resource "moon.png")))
  (when (= com.badlogic.gdx.utils.SharedLibraryLoader/os
           com.badlogic.gdx.utils.Os/MacOsX)
    (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (clojure.gdx.backends.lwjgl/application (reify clojure.gdx.application/Listener
                                            (create [_]
                                              (reset! state (cdq.application.create/context)))

                                            (dispose [_]
                                              (cdq.application.dispose/context @state))

                                            (pause [_])

                                            (render [_]
                                              (when (seq @runnables)
                                                (println "Execute " (count @runnables) "runnables.")
                                                (swap! state (fn [context]
                                                               (reduce (fn [context f] (f context))
                                                                       context
                                                                       @runnables)))
                                                (reset! runnables []))
                                              (swap! state cdq.application.render/context))

                                            (resize [_ width height]
                                              (cdq.application.resize/context @state width height))

                                            (resume [_]))
                                          {:title "Cyber Dungeon Quest"
                                           :windowed-mode {:width 1440
                                                           :height 900}
                                           :foreground-fps 60
                                           :opengl-emulation {:gl-version :gl20
                                                              :gles-3-major-version 3
                                                              :gles-3-minor-version 2}}))
