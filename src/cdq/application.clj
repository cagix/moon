(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils Disposable SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport Viewport)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(def state (atom nil))

(def ^:private runnables (atom []))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)
        create-context! (requiring-resolve (:create-context! config))
        render-pipeline (map requiring-resolve (:render-pipeline config))]
    (doseq [ns (:requires config)]
      #_(println "requiring " ns)
      (require ns))
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (io/resource (:dock-icon (:mac-os config)))))
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (reset! state (create-context! config)))

                          (dispose []
                            (doseq [[k obj] @state]
                              (if (instance? Disposable obj)
                                (do
                                 #_(println "Disposing:" k)
                                 (Disposable/.dispose obj))
                                #_(println "Not Disposable: " k ))))

                          (render []
                            (swap! state (fn [context]
                                           (reduce (fn [context f]
                                                     (f context))
                                                   context
                                                   render-pipeline)))
                            (doseq [f @runnables]
                              (f @state))
                            (reset! runnables []))

                          (resize [width height]
                            #_(doseq [[k obj] @state]
                              (if (satisfies? cdq.lifecycle/Resizable obj)
                                (do
                                 #_(println "resizing" k)
                                 (cdq.lifecycle/resize! obj width height))
                                #_(println "Not resizable " k )))
                            (let [context @state]
                              (Viewport/.update (:cdq.graphics/ui-viewport    context) width height true)
                              (Viewport/.update (:cdq.graphics/world-viewport context) width height false))))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:width  (:windowed-mode config))
                                            (:height (:windowed-mode config)))
                          (.setForegroundFPS (:foreground-fps config))))))

(defn post-runnable! [f]
  (swap! runnables conj f))
