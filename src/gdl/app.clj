(ns gdl.app
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]
            [gdl.utils :refer [defsystem install read-edn-resource]])
  (:gen-class))

(def state (atom nil))

(defn post-runnable [f]
  (gdx/post-runnable @state #(f @state)))

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
    (install {:optional [#'create
                         #'dispose
                         #'resize]}
             k)))

(defn start
  "A transaction is a `(fn [context] context)`, which can emit also side-effects or return a new context."
  [{:keys [config context transactions]}]
  (install-context-components context)
  (let [txs (doall (map load-tx transactions))]
    (lwjgl/start config
                 (reify lwjgl/Application
                   (create [_]
                     (reset! state (safe-create-into (gdx/context) context)))

                   (dispose [_]
                     (run! dispose @state))

                   (render [_]
                     (swap! state reduce-transact txs))

                   (resize [_ width height]
                     (run! #(resize % width height) @state))))))

(defn -main
  "Calls [[start]] with `\"gdl.app.edn\"`."
  []
  (start (read-edn-resource "gdl.app.edn")))
