(ns gdl.app
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.java.io :as io]
            [gdl.utils :as utils])
  (:import (com.badlogic.gdx Application
                             ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defprotocol Listener
  (create  [_ config])
  (dispose [_]) ; ! aaaahhhh 'dispose' ! same ... same name same foozoz -> _LANGUAGE_ is words verbs nouns
  ; the nouns are there
  ; the verbs need work ?
  (render  [_])
  (resize  [_ width height]))

(defn start* [state listener config]
  (when-let [icon (:icon config)]
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource icon))))
  (when (and SharedLibraryLoader/isMac
             (:glfw-async-on-mac-osx? config))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (reset! state (create (utils/safe-merge listener (gdx/context))
                                                config)))
                        (dispose []
                          (doseq [[k value] @state
                                  :when (and (not= (namespace k) "clojure.gdx") ; don't dispose internal classes, that gets handled inside Lwjgl3Window/.dispose already, otherwise crashes in tools namespace reloading workflow on dispose
                                             ; or make the gdx/context assoc as 'gdx' ?
                                             (satisfies? utils/Disposable value))]
                            (utils/dispose value)))
                        (render []
                          (swap! state render))
                        (resize [width height]
                          (swap! state resize width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:width config) (:height config))
                        (.setForegroundFPS (:fps config)))))

(defn start
  ([state listener]
   (start state listener "config.edn"))
  ([state listener edn-config]
   (start* state
           listener
           (-> edn-config io/resource slurp edn/read-string))))

(defn post-runnable [application runnable]
  (Application/.postRunnable (:clojure.gdx/app application) runnable))
