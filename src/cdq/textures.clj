(ns cdq.textures
  (:require [clojure.gdx.graphics.texture :as texture]))

; FIXME this can be memoized
; also good for tiled-map tiles they have to be memoized too
(defn image->texture-region
  [textures {:keys [image/file image/bounds]}]
  (assert file)
  (assert (contains? textures file))
  (let [texture (get textures file)]
    (if bounds
      (texture/region texture bounds)
      (texture/region texture))))
