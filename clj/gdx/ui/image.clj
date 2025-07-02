(ns gdx.ui.image
  (:import (com.badlogic.gdx.scenes.scene2d.ui Image)))

(defn set-drawable! [image-widget drawable]
  (Image/.setDrawable image-widget drawable))
