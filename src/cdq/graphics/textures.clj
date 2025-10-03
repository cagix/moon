(ns cdq.graphics.textures
  (:require [cdq.graphics]
            [com.badlogic.gdx.graphics :as graphics]
            [com.badlogic.gdx.graphics.texture :as texture]))

(defn create
  [{:keys [graphics/core]
    :as graphics}
   textures-to-load]
  (extend-type (class graphics)
    cdq.graphics/Textures
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
                    [path (graphics/texture core file-handle)]))))
