(ns gdl.graphics.pixmap
  "
  A Pixmap represents an image in memory. It has a width and height expressed in pixels as well as a Pixmap.Format specifying the number and order of color components per pixel. Coordinates of pixels are specified with respect to the top left corner of the image, with the x-axis pointing to the right and the y-axis pointing downwards.

  By default all methods use blending. You can disable blending with setBlending(Blending), which may reduce blitting time by ~30%. The drawPixmap(Pixmap, int, int, int, int, int, int, int, int) method will scale and stretch the source image to a target image. There either nearest neighbour or bilinear filtering can be used.

  A Pixmap stores its data in native heap memory. It is mandatory to call dispose() when the pixmap is no longer needed, otherwise memory leaks will result
  "
  (:import (com.badlogic.gdx.graphics Texture
                                      Pixmap
                                      Pixmap$Format)))

(defn set-color! [^Pixmap pixmap [r g b a]]
  (.setColor pixmap r g b a))

(defn draw-pixel! [^Pixmap pixmap x y]
  (.drawPixel pixmap x y))

(defn texture [^Pixmap pixmap]
  (Texture. pixmap))
