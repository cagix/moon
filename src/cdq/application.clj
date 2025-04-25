(ns cdq.application
  (:require cdq.impl.context
            cdq.impl.effects
            cdq.render
            cdq.utils
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport Viewport)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(def state (atom nil))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (io/resource (:dock-icon (:mac-os config)))))
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (reset! state (cdq.impl.context/create! config)))

                          (dispose []
                            (doseq [[k value] @state]
                              (if (satisfies? cdq.utils/Disposable value)
                                (do
                                 #_(println "Disposing:" k)
                                 (cdq.utils/dispose! value))
                                #_(println "Not Disposable: " k ))))

                          (render []
                            (swap! state (fn [context]
                                           (reduce (fn [context f]
                                                     (f context))
                                                   context
                                                   [cdq.render/assoc-active-entities
                                                    cdq.render/set-camera-on-player
                                                    cdq.render/clear-screen!
                                                    cdq.render/render-tiled-map!
                                                    cdq.render/draw-on-world-view!
                                                    cdq.render/render-stage!
                                                    cdq.render/player-state-input
                                                    cdq.render/update-mouseover-entity!
                                                    cdq.render/update-paused!
                                                    cdq.render/when-not-paused!

                                                    ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                                                    cdq.render/remove-destroyed-entities!

                                                    cdq.render/camera-controls!
                                                    cdq.render/window-controls!]))))

                          (resize [width height]
                            (let [context @state]
                              (Viewport/.update (:gdl.graphics/ui-viewport    context) width height true)
                              (Viewport/.update (:gdl.graphics/world-viewport context) width height false))))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:width  (:windowed-mode config))
                                            (:height (:windowed-mode config)))
                          (.setForegroundFPS (:foreground-fps config))))))

(defn post-runnable [f]
  (.postRunnable Gdx/app (fn [] (f @state))))
