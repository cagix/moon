(ns forge.assets
  (:refer-clojure :exclude [get])
  (:require [clojure.gdx.assets :as assets]))

(def ^:private manager)

(defn get [asset-path]
  (clojure.core/get manager asset-path))

(defn init
  "Assets are a collection of vectors `[file class]`.
  All assets are loaded immediately.
  Has to be disposed."
  [assets]
  (let [manager (assets/manager)]
    (doseq [[file class] assets]
      (assets/load manager file class))
    (assets/finish-loading manager)
    (bind-root #'manager manager)))

(defn dispose []
  (.dispose manager))

(defn all-of-class
  "Returns all asset paths with the specific class."
  [class]
  (filter #(= (assets/asset-type manager %) class)
          (assets/asset-names manager)))
