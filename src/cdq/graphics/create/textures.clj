(ns cdq.graphics.create.textures
  (:require [cdq.graphics.textures]
            [clojure.gdx.graphics.texture :as texture]))

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
          (texture/region texture bounds)
          (texture/region texture)))))
  (assoc graphics :graphics/textures
         (into {} (for [[path file-handle] textures-to-load]
                    [path (texture/create file-handle)]))))
