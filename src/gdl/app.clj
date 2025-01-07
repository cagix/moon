(ns gdl.app
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.gdx.vis-ui :as vis-ui]
            [gdl.app.create :as create]
            [gdl.utils :refer [defsystem install read-edn-resource]])
  (:gen-class))

(def state (atom nil))

(defn post-runnable [f]
  (gdx/post-runnable @state #(f @state)))

(defsystem create)
(defmethod create :default [[_ v] _context]
  v)

(defn- safe-create-into [context components]
  (reduce (fn [context [k v]]
            (assert (not (contains? context k)))
            (assoc context k (create [k v] context)))
          context
          components))

(defn- reduce-transact [value fns]
  (reduce (fn [value f]
            (f value))
          value
          fns))

(defn- load-tx [tx-sym]
  (require (symbol (namespace tx-sym)))
  (resolve tx-sym))

(defn- install-context-components [context]
  (doseq [k (map first context)]
    (install {:optional [#'create]}
             k)))

(defn start
  "A transaction is a `(fn [context] context)`, which can emit also side-effects or return a new context."
  [{:keys [config gdl-config context transactions]}]
  (install-context-components context)
  (let [txs (doall (map load-tx transactions))]
    (lwjgl/start config
                 (reify lwjgl/Application
                   (create [_]
                     (reset! state (safe-create-into (create/context (gdx/context) gdl-config)
                                                     context)))

                   (dispose [_]
                     (let [context @state]
                       ; TODO dispose :gdl.context/sd-texture
                       (gdx/dispose (:gdl.context/assets context))
                       (gdx/dispose (:gdl.context/batch  context))
                       (run! gdx/dispose (vals (:gdl.context/cursors context)))
                       (gdx/dispose (:gdl.context/default-font context))
                       (gdx/dispose (:gdl.context/stage context))
                       (vis-ui/dispose)
                       (gdx/dispose (:cdq.context/tiled-map context)))) ; TODO ! this also if world restarts !!

                   (render [_]
                     (swap! state reduce-transact txs))

                   (resize [_ width height]
                     (let [context @state]
                       (gdx/resize (:gdl.context/viewport       context) width height :center-camera? true)
                       (gdx/resize (:gdl.context/world-viewport context) width height :center-camera? false)))))))

(defn -main
  "Calls [[start]] with `\"gdl.app.edn\"`."
  []
  (start (read-edn-resource "gdl.app.edn")))
