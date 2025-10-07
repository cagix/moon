(ns cdq.graphics.create.textures
  (:require [cdq.graphics.textures]
            [clojure.gdx.graphics.texture :as texture])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn create
  [graphics textures-to-load]
  (extend-type (class graphics)
    cdq.graphics.textures/Textures
    (texture-region [{:keys [graphics/textures]}
                     {:keys [image/file image/bounds]}]
      (assert file)
      (assert (contains? textures file))
      (let [^Texture texture (get textures file)]
        (if bounds
          (let [[x y w h] bounds]
            (TextureRegion. texture (int x) (int y) (int w) (int h)))
          (TextureRegion. texture)))))
  (assoc graphics :graphics/textures
         (into {} (for [[path file-handle] textures-to-load]
                    [path (texture/create file-handle)]))))
