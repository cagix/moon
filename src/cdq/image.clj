(ns cdq.image
  (:require [clojure.gdx.graphics.texture :as texture]))

(defn texture-region
  [{:keys [image/file image/bounds]} textures]
  (assert file)
  (assert (contains? textures file))
  (let [texture (get textures file)]
    (if bounds
      (texture/region texture bounds)
      (texture/region texture))))
