(ns gdl.application
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)))

(defn start! [config]
  (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          ((:create config) {:ctx/audio    Gdx/audio
                                             :ctx/files    Gdx/files
                                             :ctx/graphics Gdx/graphics
                                             :ctx/input    Gdx/input}))
                        (dispose [_]
                          ((:dispose config)))
                        (render [_]
                          ((:render config)))
                        (resize [_ width height]
                          ((:resize config) width height))
                        (pause [_])
                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setWindowedMode (:width (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setTitle (:title config))
                        (.setForegroundFPS (:foreground-fps config)))))

(defn post-runnable! [f]
  (.postRunnable Gdx/app f))
