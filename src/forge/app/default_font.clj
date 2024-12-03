(ns ^:no-doc forge.app.default-font
  (:require [clojure.gdx.graphics.g2d.freetype :as freetype]
            [forge.core :refer :all]))

(defmethods :app/default-font
  (app-create [[_ font]]
    (bind-root #'default-font (freetype/font font)))
  (app-dispose [_]
    (.dispose default-font)))
