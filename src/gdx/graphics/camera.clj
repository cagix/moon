(ns gdx.graphics.camera
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn set-position! [^OrthographicCamera this [x y]]
  ; z optional for 2D games?
  ; (set! (.position this) (Vector3. x y z))
  (set! (.x (.position this)) (float x))
  (set! (.y (.position this)) (float y))
  (.update this))

(defn set-zoom! [^OrthographicCamera this amount]
  (set! (.zoom this) amount)
  (.update this))
