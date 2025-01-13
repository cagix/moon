(ns clojure.gdx.backends.lwjgl
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)))

(defn application [{:keys [title
                           window-width
                           window-height
                           foreground-fps
                           create
                           dispose
                           render
                           resize]}]
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create))

                        (dispose []
                          (dispose))

                        (render []
                          (render))

                        (resize [width height]
                          (resize width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode window-width
                                          window-height)
                        (.setForegroundFPS foreground-fps))))
