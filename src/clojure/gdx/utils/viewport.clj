(ns clojure.gdx.utils.viewport
  (:import (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils.viewport Viewport)))

(defn unproject
  "Transforms the specified screen coordinate to world coordinates.

  Returns:
  The vector that was passed in, transformed to world coordinates.
  See Also:

  Camera.unproject(Vector3)"
  [viewport x y]
  (let [v2 (Viewport/.unproject viewport (Vector2. x y))]
    [(.x v2) (.y v2)]))
