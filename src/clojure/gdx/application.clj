(ns clojure.gdx.application
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration)))

(defn start!
  [{:keys [title
           window
           fps
           create!
           dispose!
           render!
           resize!]}]
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (create! {:audio    Gdx/audio
                                    :files    Gdx/files
                                    :graphics Gdx/graphics
                                    :input    Gdx/input}))

                        (dispose [_]
                          (dispose!))

                        (render [_]
                          (render!))

                        (resize [_ width height]
                          (resize! width height))

                        (pause [_])

                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width window)
                                          (:height window))
                        (.setForegroundFPS fps))))
