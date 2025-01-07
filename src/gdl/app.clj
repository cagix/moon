(ns gdl.app
  (:require [clojure.gdx :refer [dispose resize]]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.gdx.vis-ui :as vis-ui]
            [gdl.app.create :as create]
            [gdl.utils :refer [read-edn-resource]]

            gdl.context
            gdl.graphics
            cdq.context
            cdq.graphics
            cdq.graphics.camera
            cdq.graphics.tiled-map)
  (:gen-class))

(def state (atom nil))

(def txs
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

(defn start [{:keys [config context]}]
  (lwjgl/start config
               (reify lwjgl/Application
                 (create [_]
                   (reset! state (create/context context)))

                 (dispose [_]
                   (vis-ui/dispose)
                   (let [context @state]
                     ; TODO dispose :gdl.context/sd-texture
                     (dispose (:gdl.context/assets context))
                     (dispose (:gdl.context/batch  context))
                     (run! dispose (vals (:gdl.context/cursors context)))
                     (dispose (:gdl.context/default-font context))
                     (dispose (:gdl.context/stage context))
                     (dispose (:cdq.context/tiled-map context)))) ; TODO ! this also if world restarts !!

                 (render [_]

                   (swap! state reduce-transact txs))

                 (resize [_ width height]
                   (let [context @state]
                     (resize (:gdl.context/viewport       context) width height :center-camera? true)
                     (resize (:gdl.context/world-viewport context) width height :center-camera? false))))))

(defn -main
  "Calls [[start]] with `\"gdl.app.edn\"`."
  []
  (start (read-edn-resource "gdl.app.edn")))
