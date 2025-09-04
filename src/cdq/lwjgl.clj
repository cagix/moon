(ns cdq.lwjgl
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3WindowConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (org.lwjgl.system Configuration)))

(defn- set-window-config-key! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :windowed-mode   (.setWindowedMode object
                                       (int (:width v))
                                       (int (:height v)))
    :title (.setTitle object (str v))))

(defn- set-application-config-key! [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :foreground-fps (.setForegroundFPS object (int v))
    (set-window-config-key! object k v)))

(let [mapping {Os/Android :android
               Os/IOS     :ios
               Os/Linux   :linux
               Os/MacOsX  :mac
               Os/Windows :windows}]
  (defn- operating-system []
    (get mapping SharedLibraryLoader/os)))

(defn start-application!
  [config
   {:keys [create!
           dispose!
           render!
           resize!]}]
  (when (= (operating-system) :mac)
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (create! {:graphics Gdx/graphics
                                    :audio    Gdx/audio
                                    :input    Gdx/input
                                    :files    Gdx/files}))
                        (dispose [_]
                          (dispose!))
                        (render [_]
                          (render!))
                        (resize [_ width height]
                          (resize! width height))
                        (pause [_])
                        (resume [_]))
                      (let [obj (Lwjgl3ApplicationConfiguration.)]
                        (doseq [[k v] config]
                          (set-application-config-key! obj k v))
                        obj)))
