(ns forge.assets.manager
  (:require [clojure.gdx.assets :as assets]))

(defn all-of-class
  "Returns all asset paths with the specific class."
  [manager class]
  (filter #(= (assets/asset-type manager %) class)
          (assets/asset-names manager)))

(defn init
  "Assets are a collection of vectors `[file class]`.
  All assets are loaded immediately.
  Has to be disposed."
  [assets]
  (let [manager (assets/manager)]
    (doseq [[file class] assets]
      (assets/load manager file class))
    (assets/finish-loading manager)
    manager))
