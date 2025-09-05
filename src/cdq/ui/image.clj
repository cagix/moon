(ns cdq.ui.image
  (:require [cdq.ui :as ui])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d.ui Image)
           (com.badlogic.gdx.scenes.scene2d.utils Drawable)
           (com.badlogic.gdx.utils Align
                                   Scaling)
           (com.kotcrab.vis.ui.widget VisImage)))

(defmulti ^:private image* type)

(defmethod image* Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod image* Texture [texture]
  (VisImage. (TextureRegion. ^Texture texture)))

(defmethod image* TextureRegion [texture-region]
  (VisImage. ^TextureRegion texture-region))

; TODO widget also make, for fill parent
(defn create
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (image* object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (ui/set-opts! opts)))
