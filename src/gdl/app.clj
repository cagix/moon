(ns gdl.app
  (:require [clojure.awt :as awt]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.java.io :as io]
            [gdl.utils :as utils])
  (:import (com.badlogic.gdx Application
                             ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)))

(defprotocol Resizable
  (resize [_ width height]))

(def state (atom nil))

(comment
 (clojure.pprint/pprint (sort (keys @state)))
 )

(defn- dispose-disposables! [context]
  (doseq [[k value] context
          :when (and (not= (namespace k) "clojure.gdx") ; don't dispose internal classes
                     (satisfies? utils/Disposable value))]
    ;(println "Disposing " k " - " value)
    (utils/dispose value)))

(defn- resize-resizables!  [context width height]
  (doseq [[k value] context
          :when (satisfies? Resizable value)]
    ;(println "Resizing " k " - " value)
    (resize value width height)))

(defn start* [create render config]
  (when-let [icon (:icon config)]
    (awt/set-taskbar-icon icon))
  (when (and SharedLibraryLoader/isMac
             (:glfw-async-on-mac-osx? config))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (reset! state (create (gdx/context) config)))

                        (dispose []
                          (dispose-disposables! @state))

                        (render []
                          (swap! state render))

                        (resize [width height]
                          (resize-resizables! @state width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:width config) (:height config))
                        (.setForegroundFPS (:fps config)))))

(defn start
  ([create render]
   (start create render "config.edn"))
  ([create render edn-config]
   (start* create
           render
           (-> edn-config io/resource slurp edn/read-string))))

(defn post-runnable [f]
  (Application/.postRunnable (:clojure.gdx/app @state)
                             #(f @state)))
