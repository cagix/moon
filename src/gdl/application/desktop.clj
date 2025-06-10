(ns gdl.application.desktop
  (:require clojure.edn
            clojure.java.io
            clojure.walk
            [gdl.application])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defn start! [config-path]
  (let [config (->> config-path
                    clojure.java.io/resource
                    slurp
                    clojure.edn/read-string
                    (clojure.walk/postwalk (fn [form]
                                             (if (symbol? form)
                                               (if (namespace form)
                                                 (requiring-resolve form)
                                                 (do
                                                  (require form)
                                                  form))
                                               form))))
        config (reify ILookup
                 (valAt [_ k]
                   (assert (contains? config k)
                           (str "Config key not found: " k))
                   (get config k)))]
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (clojure.java.io/resource (:gdl.application/taskbar-icon config)))))
    (Lwjgl3Application. (gdl.application/create-application-listener config)
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:gdl.application/title config))
                          (.setWindowedMode (:width  (:gdl.application/windowed-mode config))
                                            (:height (:gdl.application/windowed-mode config)))
                          (.setForegroundFPS (:gdl.application/foreground-fps config))))))
