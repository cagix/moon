(ns forge.app.default-font
  (:require [clojure.gdx.graphics.g2d.freetype :as freetype])
  (:require [forge.core :refer [bind-root
                                dispose
                                default-font]]))

(defn create [[_ font]]
  (bind-root default-font (freetype/generate-font font)))

(defn destroy [_]
  (dispose default-font))
