(ns gdl.app
  (:require [clojure.java.io :as io]

            [clojure.gdx :refer [dispose resize]]
            [clojure.gdx.vis-ui :as vis-ui]
            [gdl.app.create :as create]
            [gdl.utils :refer [read-edn-resource]]

            gdl.context
            gdl.graphics
            cdq.context
            cdq.graphics
            cdq.graphics.camera
            cdq.graphics.tiled-map)
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration))
  (:gen-class))

(def state (atom nil))

(def ^:private txs
  [gdl.graphics/clear-screen
   cdq.graphics.camera/set-on-player-position
   cdq.graphics.tiled-map/render
   cdq.graphics/draw-world-view
   gdl.graphics/draw-stage

   ; updates
   gdl.context/update-stage
   cdq.context/handle-player-input
   cdq.context/update-mouseover-entity
   cdq.context/update-paused-state
   cdq.context/progress-time-if-not-paused
   cdq.context/remove-destroyed-entities  ; do not pause this as for example pickup item, should be destroyed.
   gdl.context/check-camera-controls
   cdq.context/check-ui-key-listeners])

(defn- reduce-transact [value fns]
  (reduce (fn [value f]
            (f value))
          value
          fns))

(defn start [config]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource (:icon config))))
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (reset! state (create/context (:context config))))

                        (dispose []
                          (vis-ui/dispose)
                          (let [context @state]
                            ; TODO dispose :gdl.context/sd-texture
                            (dispose (:gdl.context/assets context))
                            (dispose (:gdl.context/batch  context))
                            (run! dispose (vals (:gdl.context/cursors context)))
                            (dispose (:gdl.context/default-font context))
                            (dispose (:gdl.context/stage context))
                            (dispose (:cdq.context/tiled-map context)))) ; TODO ! this also if world restarts !!

                        (render []
                          (swap! state reduce-transact txs))

                        (resize [width height]
                          (let [context @state]
                            (resize (:gdl.context/viewport       context) width height :center-camera? true)
                            (resize (:gdl.context/world-viewport context) width height :center-camera? false))))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:window-width config)
                                          (:window-height config))
                        (.setForegroundFPS (:fps config)))))

(defn -main
  "Calls [[start]] with `\"gdl.app.edn\"`."
  []
  (start (read-edn-resource "gdl.app.edn")))
