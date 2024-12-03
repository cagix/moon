(ns forge.app.default-font
  (:require [clojure.gdx.graphics.g2d.freetype :as freetype]
            [forge.context :as context]
            [forge.lifecycle :as lifecycle]
            [forge.system :refer [defmethods bind-root]]))

(defmethods :app/default-font
  (lifecycle/create [[_ font]]
    (bind-root #'context/default-font (freetype/font font)))
  (lifecycle/dispose [_]
    (.dispose context/default-font)))
