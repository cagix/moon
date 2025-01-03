(ns gdl.app
  (:require [clojure.component :refer [defsystem install]]
            [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]))

(defsystem create)
(defmethod create :default [[_ v] _context]
  v)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem resize)
(defmethod resize :default [_ width height])

(defn- safe-create-into [context components]
  (reduce (fn [context [k v]]
            (assert (not (contains? context k)))
            (assoc context k (create [k v] context)))
          context
          components))

(def state (atom nil))

(defn start [app-config components render]
  (lwjgl/start app-config
               (reify lwjgl/Application
                 (create [_]
                   (reset! state (safe-create-into (gdx/context) components)))

                 (dispose [_]
                   (run! dispose @state))

                 (render [_]
                   (swap! state render))

                 (resize [_ width height]
                   (run! #(resize % width height) @state)))))

(def systems
  {:optional [#'create
              #'dispose
              #'resize]})

(doseq [[ns-sym k] '{gdl.context.assets             :gdl.context/assets
                     gdl.context.batch              :gdl.context/batch
                     gdl.context.cursors            :gdl.context/cursors
                     gdl.context.db                 :gdl.context/db
                     gdl.context.default-font       :gdl.context/default-font
                     gdl.context.shape-drawer       :gdl.context/shape-drawer
                     gdl.context.stage              :gdl.context/stage
                     gdl.context.tiled-map-renderer :gdl.context/tiled-map-renderer
                     gdl.context.ui                 :gdl.context/ui
                     gdl.context.viewport           :gdl.context/viewport
                     gdl.context.world-unit-scale   :gdl.context/world-unit-scale
                     gdl.context.world-viewport     :gdl.context/world-viewport}]
  (install systems ns-sym k))
