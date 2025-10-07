(ns cdq.graphics.create.textures
  (:require [cdq.graphics.textures]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]))

(defn create
  [graphics textures-to-load]
  (extend-type (class graphics)
    cdq.graphics.textures/Textures
    (texture-region [{:keys [graphics/textures]}
                     {:keys [image/file image/bounds]}]
      (assert file)
      (assert (contains? textures file))
      (let [texture (get textures file)]
        (if bounds
          (texture-region/create texture bounds)
          (texture-region/create texture)))))
  (assoc graphics :graphics/textures
         (into {} (for [[path file-handle] textures-to-load]
                    [path (texture/create file-handle)]))))
