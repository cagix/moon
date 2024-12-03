(ns ^:no-doc forge.app.default-font
  (:require [clojure.gdx.graphics.g2d.freetype :as freetype]
            [forge.system :as system]))

(defmethods :app/default-font
  (system/create [[_ font]]
    (bind-root #'system/default-font (freetype/font font)))
  (system/dispose [_]
    (.dispose system/default-font)))
