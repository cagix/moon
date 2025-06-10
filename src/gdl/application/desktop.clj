(ns gdl.application.desktop
  (:require clojure.edn
            clojure.java.io
            clojure.walk
            [gdl.application])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)
           clojure.lang.ILookup))

; with the direct path I don't have to invent names for things
; and don't need to see the namespace context for names
; everything is clear and direct.
; also stop with this 'if' and 'when'
; why does everything need a fucking name
; each component tx can be an anonymous fn has a name through the keyword only
; also context transactions only w. 'gdl.c/'
; and them make application-listener separate because it became immense
(defn -main [config-path]
  (let [config (let [m (->> config-path
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
                                                       form))))]
                 (reify ILookup
                   (valAt [_ k]
                     (assert (contains? m k)
                             (str "Config key not found: " k))
                     (get m k))))]
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
