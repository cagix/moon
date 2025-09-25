(ns gdl.graphics.viewport)

(defprotocol Viewport
  (update! [_ width height {:keys [center?]}])
  (unproject [_ touch-coordinates]
             "Transforms the specified touch coordinates (`[x y]`) to world coordinates.
             The x- and y-coordinate of vec are assumed to be in touch coordinates (origin is the top left corner, y pointing down, x pointing to the right)

             Returns the world-coordinates as `[x y]`."))
