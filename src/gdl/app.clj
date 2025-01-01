(ns gdl.app
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.component :refer [defsystem]]))

(defsystem create)
(defmethod create :default [[_ v] _context]
  v)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem resize)
(defmethod resize :default [_ width height])

(defn- create-into [context components]
  (reduce (fn [context [k v]]
            (assert (not (contains? context k)))
            (assoc context k (create [k v] context)))
          context
          components))

(def state (atom nil))

(comment
 (clojure.pprint/pprint (sort (keys @state)))

 )

(defn start [app-config components render]
  (lwjgl/start app-config
               (reify lwjgl/Application
                 (create [_]
                   (reset! state (create-into (gdx/context) components)))

                 (dispose [_]
                   (run! dispose @state))

                 (render [_]
                   (swap! state render))

                 (resize [_ width height]
                   (run! #(resize % width height) @state)))))
