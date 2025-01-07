(ns gdl.app
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.gdx.vis-ui :as vis-ui]
            [gdl.app.create :as create]
            [gdl.utils :refer [read-edn-resource]])
  (:gen-class))

(def state (atom nil))

(defn post-runnable [f]
  (gdx/post-runnable @state #(f @state)))

(defn- reduce-transact [value fns]
  (reduce (fn [value f]
            (f value))
          value
          fns))

(defn- load-tx [tx-sym]
  (require (symbol (namespace tx-sym)))
  (resolve tx-sym))

(defn start
  "A transaction is a `(fn [context] context)`, which can emit also side-effects or return a new context."
  [{:keys [config context transactions]}]
  (let [txs (doall (map load-tx transactions))]
    (lwjgl/start config
                 (reify lwjgl/Application
                   (create [_]
                     (reset! state (create/context context)))

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
