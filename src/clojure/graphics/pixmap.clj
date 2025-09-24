(ns clojure.graphics.pixmap
  "
A Pixmap represents an image in memory. It has a width and height expressed in pixels as well as a Pixmap.Format specifying the number and order of color components per pixel. Coordinates of pixels are specified with respect to the top left corner of the image, with the x-axis pointing to the right and the y-axis pointing downwards.

By default all methods use blending. You can disable blending with setBlending(Blending), which may reduce blitting time by ~30%. The drawPixmap(Pixmap, int, int, int, int, int, int, int, int) method will scale and stretch the source image to a target image. There either nearest neighbour or bilinear filtering can be used.

A Pixmap stores its data in native heap memory. It is mandatory to call dispose() when the pixmap is no longer needed, otherwise memory leaks will result
  "
  )

(defprotocol Pixmap
  (set-color! [_ [r g b a]]
              "
             Sets the color for the following drawing operations.

Parameters:
    r - The red component.
    g - The green component.
    b - The blue component.
    a - The alpha component.
              "
              )
  (draw-pixel! [_ x y]
               "

Draws a pixel at the given location with the current color.

Parameters:
    x - the x-coordinate
    y - the y-coordinate

               "
               )
  (texture [_]
           "see `clojure.graphics.texture`"
           ))
