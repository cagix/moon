(ns clojure.gdx.scene2d.utils.texture-region-drawable
  (:import (com.badlogic.gdx.scenes.scene2d.utils TextureRegionDrawable)))

(defn create [texture-region]
  (TextureRegionDrawable. texture-region))
