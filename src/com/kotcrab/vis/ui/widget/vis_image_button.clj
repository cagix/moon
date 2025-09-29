(ns com.kotcrab.vis.ui.widget.vis-image-button
  (:import (com.badlogic.gdx.scenes.scene2d.utils Drawable)
           (com.kotcrab.vis.ui.widget VisImageButton)))

(defn create [drawable]
  (VisImageButton. ^Drawable drawable))
