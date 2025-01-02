(ns gdl.assets
  (:require [clojure.gdx.assets :as assets]))

(defn all-of-type
  "Returns all asset paths with the specific asset-type."
  [manager asset-type]
  (filter #(= (assets/type manager %) asset-type)
          (assets/names manager)))
